import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

def readDataArray(fname):
    arr = []
    with open(fname, 'r') as f:
        for line in f.readlines():
            arr.append(int(line))
    return arr

def readDataArrayFloat(fname):
    arr = []
    with open(fname, 'r') as f:
        for line in f.readlines():
            arr.append(float(line))
    return arr

FILE_NAMES_OCC = [
    'occ_dbpedia_factbook.txt',
    'occ_student_loan.txt',
    'occ_elti.txt',
    'occ_family_medium.txt',
    'occ_family_simple.txt',
]

FILE_NAMES_LOC = [
    'loc_dbpedia_factbook.txt',
    'loc_student_loan.txt',
    'loc_elti.txt',
    'loc_family_medium.txt',
    'loc_family_simple.txt',
]

FILE_NAMES_JCD = [
    'jcd_dbpedia_factbook.txt',
    'jcd_student_loan.txt',
    'jcd_elti.txt',
    'jcd_family_medium.txt',
    'jcd_family_simple.txt',
]

PLOT_LABELS = [
    "DBpedia\nfactbook",
    "Student\nLoan",
    "Elti",
    "Family\n(Medium)",
    "Family\n(Simple)"
]

COMP_RATIOS = [
    1.29,
    1.33,
    1.51,
    1.82,
    2.22
]

# Plot BoxChart for Symbol Occurrences
figure = plt.figure(figsize=(1800/300, 1200/300), dpi=300)
data = []
for i in range(5):
    data.append(readDataArray(FILE_NAMES_OCC[i]))

figure, dist = plt.subplots()
dist.boxplot(data, showfliers=False)
dist.set_ylabel('Const. Occurrences')
plt.xticks([1, 2, 3, 4, 5], PLOT_LABELS)

comp_ratio = dist.twinx()
p2, = comp_ratio.plot([1, 2, 3, 4, 5], COMP_RATIOS, "ro")
comp_ratio.set_ylim(1, 2.5)
comp_ratio.set_ylabel('Comp. Ratio', color='tab:red')
comp_ratio.yaxis.label.set_color(p2.get_color())
plt.savefig("Distribution of the Number of Constant Occurences.png")


# Plot BoxChart for Symbol Locations
figure = plt.figure(figsize=(1800/300, 1200/300), dpi=300)
data = []
for i in range(5):
    data.append(readDataArray(FILE_NAMES_LOC[i]))

figure, dist = plt.subplots()
dist.boxplot(data, showfliers=False)
dist.set_ylabel('Const. Locations')
plt.xticks([1, 2, 3, 4, 5], PLOT_LABELS)

comp_ratio = dist.twinx()
p2, = comp_ratio.plot([1, 2, 3, 4, 5], COMP_RATIOS, "ro")
comp_ratio.set_ylim(1, 2.5)
comp_ratio.set_ylabel('Comp. Ratio', color='tab:red')
comp_ratio.yaxis.label.set_color(p2.get_color())
plt.savefig("Distribution of the Number of Constant Locations.png")


# Plot BoxChart for Predicate Argument Similarities
figure = plt.figure(figsize=(1800/300, 1200/300), dpi=300)
data = []
for i in range(5):
    data.append(readDataArrayFloat(FILE_NAMES_JCD[i]))

figure, dist = plt.subplots()
dist.boxplot(data, showfliers=False)
dist.set_ylabel('Pred. Arg. Similarities')
plt.xticks([1, 2, 3, 4, 5], PLOT_LABELS)

comp_ratio = dist.twinx()
p2, = comp_ratio.plot([1, 2, 3, 4, 5], COMP_RATIOS, "ro")
comp_ratio.set_ylim(1, 2.5)
comp_ratio.set_ylabel('Comp. Ratio', color='tab:red')
comp_ratio.yaxis.label.set_color(p2.get_color())
plt.savefig("Distribution of the Number of Predicate Argument Similarities.png")

# # Plot Compression Ratios
# figure = plt.figure(figsize=(1800/300, 1200/300), dpi=300)
# plt.plot([1, 2, 3, 4, 5], COMP_RATIOS, "ko", label="Comp. Ratio")
# plt.xticks([1, 2, 3, 4, 5], PLOT_LABELS)
# plt.savefig("Compression Ratio.png")