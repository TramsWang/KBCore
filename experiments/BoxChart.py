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

DATASET_INCLUDE = [
    ("DBpedia factbook", 0),
    ("Student Loan", 1),
    ("Elti", 2),
    ("Dunur", 3),
    ("Family (Medium)", 4),
    ("Family (Simple)", 5),
    ("WebKB Cornell", 6),
    ("WebKB Texas", 7),
    ("WebKB Washington", 8),
    ("WebKB Wisconsin", 9)
]

FILE_NAMES_OCC = [
    'occ/occ_dbpedia_factbook.txt',
    'occ/occ_student_loan.txt',
    'occ/occ_elti.txt',
    'occ/occ_dunur.txt',
    'occ/occ_family_medium.txt',
    'occ/occ_family_simple.txt',
    'occ/occ_webkb_cornell.txt',
    'occ/occ_webkb_texas.txt',
    'occ/occ_webkb_washington.txt',
    'occ/occ_webkb_wisconsin.txt',
]

FILE_NAMES_LOC = [
    'loc/loc_dbpedia_factbook.txt',
    'loc/loc_student_loan.txt',
    'loc/loc_elti.txt',
    'loc/loc_dunur.txt',
    'loc/loc_family_medium.txt',
    'loc/loc_family_simple.txt',
    'loc/loc_webkb_cornell.txt',
    'loc/loc_webkb_texas.txt',
    'loc/loc_webkb_washington.txt',
    'loc/loc_webkb_wisconsin.txt',
]

FILE_NAMES_JCD = [
    'jcd/jcd_dbpedia_factbook.txt',
    'jcd/jcd_student_loan.txt',
    'jcd/jcd_elti.txt',
    'jcd/jcd_dunur.txt',
    'jcd/jcd_family_medium.txt',
    'jcd/jcd_family_simple.txt',
    'jcd/jcd_webkb_cornell.txt',
    'jcd/jcd_webkb_texas.txt',
    'jcd/jcd_webkb_washington.txt',
    'jcd/jcd_webkb_wisconsin.txt',
]

PLOT_LABELS = [
    "DPf",
    "SL",
    "El",
    "Du",
    "FM",
    "FS",
    "WKC",
    "WKT",
    "WKa",
    "WKi"
]

COMP_RATIOS = [
    1.29,
    1.33,
    1.51,
    1,
    1.82,
    2.22,
    1,
    1,
    1,
    1
]

idxs = []
used_labels = []
for dataset, idx in DATASET_INCLUDE:
    idxs.append(idx)
    used_labels.append(PLOT_LABELS[idx])
xticks = list(range(1, len(idxs) + 1))
# print(idxs)
print(used_labels)
# print(xticks)

# Plot BoxChart for Symbol Occurrences
figure = plt.figure(figsize=(1800/300, 1200/300), dpi=300)
data = []
for idx in idxs:
    data.append(readDataArray(FILE_NAMES_OCC[idx]))

figure, dist = plt.subplots()
dist.boxplot(data, showfliers=False)
dist.set_ylabel('Const. Occurrences')
plt.xticks(xticks, used_labels)

comp_ratio = dist.twinx()
p2, = comp_ratio.plot(xticks, COMP_RATIOS, "ro")
comp_ratio.set_ylim(1, 2.5)
comp_ratio.set_ylabel('Comp. Ratio', color='tab:red')
comp_ratio.yaxis.label.set_color(p2.get_color())
plt.savefig("Distribution of the Number of Constant Occurences.png")


# Plot BoxChart for Symbol Locations
figure = plt.figure(figsize=(1800/300, 1200/300), dpi=300)
data = []
for idx in idxs:
    data.append(readDataArray(FILE_NAMES_LOC[idx]))

figure, dist = plt.subplots()
dist.boxplot(data, showfliers=False)
dist.set_ylabel('Const. Locations')
plt.xticks(xticks, used_labels)

comp_ratio = dist.twinx()
p2, = comp_ratio.plot(xticks, COMP_RATIOS, "ro")
comp_ratio.set_ylim(1, 2.5)
comp_ratio.set_ylabel('Comp. Ratio', color='tab:red')
comp_ratio.yaxis.label.set_color(p2.get_color())
plt.savefig("Distribution of the Number of Constant Locations.png")


# Plot BoxChart for Predicate Argument Similarities
figure = plt.figure(figsize=(1800/300, 1200/300), dpi=300)
data = []
for idx in idxs:
    data.append(readDataArrayFloat(FILE_NAMES_JCD[idx]))

figure, dist = plt.subplots()
dist.boxplot(data, showfliers=False)
dist.set_ylabel('Pred. Arg. Similarities')
plt.xticks(xticks, PLOT_LABELS)

comp_ratio = dist.twinx()
p2, = comp_ratio.plot(xticks, COMP_RATIOS, "ro")
comp_ratio.set_ylim(1, 2.5)
comp_ratio.set_ylabel('Comp. Ratio', color='tab:red')
comp_ratio.yaxis.label.set_color(p2.get_color())
plt.savefig("Distribution of the Number of Predicate Argument Similarities.png")

# # Plot Compression Ratios
# figure = plt.figure(figsize=(1800/300, 1200/300), dpi=300)
# plt.plot([1, 2, 3, 4, 5], COMP_RATIOS, "ko", label="Comp. Ratio")
# plt.xticks([1, 2, 3, 4, 5], PLOT_LABELS)
# plt.savefig("Compression Ratio.png")