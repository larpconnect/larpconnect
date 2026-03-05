import xml.etree.ElementTree as ET

tree = ET.parse('server/build/reports/jacoco/test/jacocoTestReport.xml')
root = tree.getroot()

for package in root.findall('package'):
    if package.attrib['name'] == 'com/larpconnect/njall/server':
        for clazz in package.findall('class'):
            if clazz.attrib['name'] == 'com/larpconnect/njall/server/DefaultMainVerticle':
                for source in package.findall('sourcefile'):
                    if source.attrib['name'] == 'DefaultMainVerticle.java':
                        for line in source.findall('line'):
                            if int(line.attrib['mi']) > 0:
                                print(f"Line {line.attrib['nr']} has missed instructions: mi={line.attrib['mi']}, ci={line.attrib['ci']}")
