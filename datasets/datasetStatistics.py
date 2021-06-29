from os import listdir
from os.path import join


print("Fname\t#F\t|B|\t|C|")
for fname in listdir("."):
    if fname.endswith("tsv"):
        functors = set()
        constants = set()
        predicates = 0
        with open(fname, 'r') as fd:
            for line in fd.readlines():
                components = line.strip().split('\t')
                functors.add(components[0])
                constants.update(components[1:])
                predicates += 1
        print("%s\t%d\t%d\t%d" % (fname, len(functors), predicates, len(constants)))