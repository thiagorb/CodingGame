class Person:
    def __init__(self, id):
        self.id = id
        self.influenceds = {}
        self.root = True
        
    def addInfluenced(self, influenced):
        self.influenceds[influenced.id] = influenced
        influenced.root = False
        
    def sucession(self):
        if not self.influenceds: return 1
        return max([p.sucession() for p in self.influenceds.values()]) + 1

class PersonCollection:
    def __init__(self):
        self.people = {}
    
    def get(self, id):
        p = self.people.get(id)
        if not p:
            p = Person(id)
            self.people[id] = p
        return p
        
    def getAll(self):
        return self.people.values()

n = int(raw_input())

people = PersonCollection()

for i in xrange(n):
    influence = [int(j) for j in raw_input().split(" ")]
    p1 = people.get(influence[0])
    p2 = people.get(influence[1])
    p1.addInfluenced(p2)

print max([p.sucession() for p in people.getAll() if p.root])