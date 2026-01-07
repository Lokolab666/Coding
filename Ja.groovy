// 1) Keep empty lines (don’t findAll/remove them before splitting)
def lines = templateFileContent.readLines().collect { it.trim() }

// If your file always has 6 header lines (like your example)
int reeStart = 6

// 2) Find the first blank line AFTER the REE section starts
int splitIndex = (reeStart..<lines.size()).find { idx -> lines[idx].isEmpty() } ?: lines.size()

// 3) REE lines = from reeStart until the blank line
def resourceEnvironmentEntries = (reeStart < splitIndex)
  ? lines[reeStart..<splitIndex].findAll { it }
  : []

// Optional: normalize ONLY the name part (before ;) removing spaces:
// "Prueba Entriesv2;config/.." => "PruebaEntriesv2;config/.."
resourceEnvironmentEntries = resourceEnvironmentEntries.collect { s ->
  def parts = s.split(';', 2)
  (parts.size() == 2) ? (parts[0].replaceAll(/\s+/, '') + ';' + parts[1]) : s
}

// 4) Custom lines = after the blank line until end
def rawCustomLines = (splitIndex + 1 < lines.size())
  ? lines[(splitIndex + 1)..<lines.size()].findAll { it }
  : []

// 5) Some custom lines can contain multiple key;value pairs in the same line.
// Split by whitespace ONLY when the next token looks like "something;something"
def customProp = rawCustomLines
  .collectMany { ln -> ln.split(/\s+(?=[^;\s]+;)/) }  // <-- important
  .collect { it.trim() }
  .findAll { it }

// Optional: same normalization for the key part before ;
customProp = customProp.collect { s ->
  def parts = s.split(';', 2)
  (parts.size() == 2) ? (parts[0].replaceAll(/\s+/, '') + ';' + parts[1]) : s
}

commonStgs.printOutput("resourceEnvironmentEntries: ${resourceEnvironmentEntries}", "G")
commonStgs.printOutput("Custom: ${customProp}", "G")
