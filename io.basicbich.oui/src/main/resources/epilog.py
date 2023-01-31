writer = csv.writer(sys.stdout)
writer.writerow(data[0].keys())
for row in data:
    writer.writerow(row.values())
