import sys

def countChars(word):
    wordMap = {}
    for c in word:
        wordMap[c] = wordMap.get(c, 0) + 1
    return wordMap

class Table:
    def __init__(self, availableChars):
        tableMap = {
            "eaionrtlsu": 1,
            "dg": 2,
            "bcmp": 3,
            "fhvwy": 4,
            "k": 5,
            "jx": 8,
            "qz": 10
        }
        self.charValues = {}
        for key in tableMap.keys():
            cValue = tableMap[key]
            for c in key:
                if c in availableChars:
                    self.charValues[c] = cValue
        self.availableChars = countChars(availableChars)
    
    
    def word_value(self, word):
        values = [self.charValues.get(c) for c in word]
        if None in values: return None
        wordMap = countChars(word)
        if any([wordMap[c] > self.availableChars[c] for c in word]): return None
        return sum(values)

n = int(raw_input())
dictionary = [raw_input() for i in xrange(n)]
availableChars = raw_input()
table = Table(availableChars)
best = [None, None]
for w in dictionary:
    value = table.word_value(w)
    if value > best[0]:
        best = [value, w]
print best[1]