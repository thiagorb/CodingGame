import java.util.*;
import java.io.*;
import java.math.*;

class Position {
    public final int x;
    public final int y;
    
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + x;
        hash = hash * 31 + y;
        return hash;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other.getClass() != Position.class) return false;
        Position p = (Position)other;
        return this.x == p.x && this.y == p.y;
    }
}

enum MapTile {
    START,
    GOAL,
    EMPTY,
    OBSTACLE_X,
    OBSTACLE_SHARP,
    MODIFIER_S,
    MODIFIER_N,
    MODIFIER_E,
    MODIFIER_W,
    BEER,
    INVERTER,
    TELEPORT;
    
    public static MapTile fromChar(char c) {
        switch (c) {
            case '@': return MapTile.START;
            case '$': return MapTile.GOAL;
            case ' ': return MapTile.EMPTY;
            case 'X': return MapTile.OBSTACLE_X;
            case '#': return MapTile.OBSTACLE_SHARP;
            case 'S': return MapTile.MODIFIER_S;
            case 'N': return MapTile.MODIFIER_N;
            case 'E': return MapTile.MODIFIER_E;
            case 'W': return MapTile.MODIFIER_W;
            case 'B': return MapTile.BEER;
            case 'I': return MapTile.INVERTER;
            case 'T': return MapTile.TELEPORT;
        }
        return null;
    }
}

interface BreakingListener {
    void breaking(Position p);
}

class Map {
    private MapTile[][] tiles;
    private Position t1;
    private Position t2;
    private List<BreakingListener> breakingListeners = new ArrayList<BreakingListener>();
    
    public Map(MapTile[][] tiles) {
        this.tiles = tiles;
    }
    
    public MapTile getTile(Position p) {
        return tiles[p.y][p.x];
    }
    
    public void breakX(Position p) {
        tiles[p.y][p.x] = MapTile.EMPTY;
        for (BreakingListener listener: breakingListeners) {
            listener.breaking(p);
        }
    }
    
    public Position teleport(Position p) {
        return p.equals(t1)? t2: t1;
    }
    
    public void setTeleport(Position t1, Position t2) {
        this.t1 = t1;
        this.t2 = t2;
    }
    
    public void addBreakingListener(BreakingListener listener) {
        breakingListeners.add(listener);
    }
}

enum BlenderDirection {
    SOUTH {
        @Override
        public Position move(Position from){
            return new Position(from.x, from.y + 1);
        }
    }, 
    EAST {
        @Override
        public Position move(Position from){
            return new Position(from.x + 1, from.y);
        }
    }, 
    NORTH {
        @Override
        public Position move(Position from){
            return new Position(from.x, from.y - 1);
        }
    }, 
    WEST {
        @Override
        public Position move(Position from){
            return new Position(from.x - 1, from.y);
        }
    };
    
    public abstract Position move(Position from);
}

class BlenderState {
    private BlenderDirection direction = BlenderDirection.SOUTH;
    private Position position;
    private Map map;
    private boolean breaker = false;
    private boolean invert = false;
    private boolean teleported = false;
    
    public BlenderState(Position position, Map map) {
        this.position = position;
        this.map = map;
    }
    
    public BlenderDirection getNextDirection() {
        for (BlenderDirection facing: getPriorities()) {
            Position facingPosition = facing.move(this.position);
            if (canMoveTo(facingPosition)) {
                return facing;
            }
        }
        return null;
    }
    
    public BlenderState performMove(BlenderDirection facing) {
        BlenderState next = new BlenderState(facing.move(position), map);
        next.invert = this.invert;
        next.breaker = this.breaker;
        next.direction = facing;
        MapTile t = map.getTile(next.position);
        switch (t) {
            case MODIFIER_S:
                next.direction = BlenderDirection.SOUTH;
                break;
            case MODIFIER_N:
                next.direction = BlenderDirection.NORTH;
                break;
            case MODIFIER_E:
                next.direction = BlenderDirection.EAST;
                break;
            case MODIFIER_W: 
                next.direction = BlenderDirection.WEST;
                break;
            case BEER:
                next.breaker = !breaker;
                break;
            case OBSTACLE_X:
                map.breakX(next.position);
                break;
            case INVERTER:
                next.invert = !invert;
                break;
            case TELEPORT:
                next.position = map.teleport(next.position);
                next.teleported = true;
                break;
        }
        return next;
    }
    
    public BlenderDirection[] getPriorities() {
        if (invert) {
            return new BlenderDirection[] {
                this.direction, BlenderDirection.WEST, BlenderDirection.NORTH, BlenderDirection.EAST, BlenderDirection.SOUTH
            };
        } else {
            return new BlenderDirection[] {
                this.direction, BlenderDirection.SOUTH, BlenderDirection.EAST, BlenderDirection.NORTH, BlenderDirection.WEST
            };
        }
    }
    
    public boolean canMoveTo(Position p) {
        MapTile tile = map.getTile(p);
        switch (tile) {
            case OBSTACLE_X:
                return breaker;
            case OBSTACLE_SHARP:
                return false;
            default:
                return true;
        }
    }
    
    public boolean foundGoal() {
        return map.getTile(position) == MapTile.GOAL;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + position.hashCode();
        hash = hash * 31 + direction.hashCode();
        hash = hash * 13 + (breaker? 1: 0);
        hash = hash * 19 + (invert? 1: 0);
        return hash;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other.getClass() != BlenderState.class) return false;
        BlenderState s = (BlenderState)other;
        return 
            this.position.equals(s.position) && 
            this.direction == s.direction &&
            this.breaker == s.breaker &&
            this.invert == s.invert;
    }
    
    public Position getPosition() {
        return this.position;
    }
    
    public BlenderDirection getDirection() {
        return this.direction;
    }
    
    public boolean getTeleported() {
        return this.teleported;
    }
}

class LoopControl implements BreakingListener {
    private Object[][] statesCollections;
    private int l;
    private int c;
    
    private void reset() {
        statesCollections = new Object[l][];
        for (int i = 0; i < l; i++) {
            statesCollections[i] = new Object[c];
            for (int j = 0; j < c; j++) {
                statesCollections[i][j] = new HashSet<BlenderState>();
            }
        }
    }
    
    public LoopControl(int l, int c) {
        this.l = l;
        this.c = c;
        reset();
    }
    
    @Override
    public void breaking(Position p) {
        reset();
    }
    
    private Collection<BlenderState> mapState(BlenderState state) {
        Position p = state.getPosition();
        return (Collection<BlenderState>)statesCollections[p.y][p.x];
    }
    
    public void addState(BlenderState state) {
        mapState(state).add(state);
    }
    
    public boolean loop(BlenderState state) {
        Collection<BlenderState> m = mapState(state);
        return m.contains(state);
    }
}

public class Solution {
    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int l = in.nextInt();
        int c = in.nextInt();
        in.nextLine();
        
        MapTile[][] tiles = new MapTile[l][];
        Position start = null;
        Position t1 = null;
        Position t2 = null;
        for (int i = 0; i < l; i++) {
            String line = in.nextLine();
            tiles[i] = new MapTile[c];
            for (int j = 0; j < c; j++) {
                tiles[i][j] = MapTile.fromChar(line.charAt(j));
                switch (tiles[i][j]) {
                    case START:
                        start = new Position(j, i);
                        break;
                    case TELEPORT:
                        if (t1 == null)
                            t1 = new Position(j, i);
                        else
                            t2 = new Position(j, i);
                        break;
                }
            }
        }
        Map map = new Map(tiles);
        if (t1 != null)
            map.setTeleport(t1, t2);
        
        LoopControl loopControl = new LoopControl(l, c);
        map.addBreakingListener(loopControl);
        BlenderState blender = new BlenderState(start, map);
        
        List<BlenderDirection> answer = new ArrayList<BlenderDirection>();
        BlenderDirection previousDirection = null;
        while (!blender.foundGoal()) {
            BlenderDirection nextDirection = blender.getNextDirection();
            if (nextDirection != previousDirection || blender.getTeleported()) {
                if (loopControl.loop(blender)) {
                    System.out.println("LOOP");
                    return;
                }
                loopControl.addState(blender);
            }
            blender = blender.performMove(nextDirection);
            answer.add(nextDirection);
            previousDirection = nextDirection;
        }
        
        for (BlenderDirection d: answer) {
            System.out.println(d);
        }
    }
}