// IMPORTANT: do NOT remove empty lines before splitting
def lines = templateFileContent.readLines().collect { it == null ? "" : it.trim() }

// Find header end (first blank line)
int headerEnd = (0..<lines.size()).find { idx -> lines[idx].isEmpty() } ?: -1

// REE starts at first non-empty after headerEnd
int reeStart = (headerEnd + 1 < lines.size())
  ? ((headerEnd + 1)..<lines.size()).find { idx -> !lines[idx].isEmpty() }
  : null

// REE ends at next blank line after reeStart
int reeEnd = (reeStart != null)
  ? ((reeStart)..<lines.size()).find { idx -> lines[idx].isEmpty() }
  : null

def resourceEnvironmentEntries = []
def rawCustomLines = []

if (reeStart != null) {
  int reeStop = (reeEnd != null) ? reeEnd : lines.size()

  resourceEnvironmentEntries = lines[reeStart..<reeStop].findAll { it }

  // Custom starts after reeEnd (skip blanks)
  int customStart = (reeEnd != null) ? reeEnd + 1 : reeStop
  if (customStart < lines.size()) {
    rawCustomLines = lines[customStart..<lines.size()].findAll { it }
  }
}

// Some custom lines may contain multiple key;value pairs on ONE line -> split them
def customProp = rawCustomLines
  .collectMany { ln -> ln.split(/\s+(?=[^;\s]+;)/) }   // split only where next token looks like "key;value"
  .collect { it.trim() }
  .findAll { it }

// Optional: remove spaces only in the KEY (before ';') so "Prueba Entriesv2" becomes "PruebaEntriesv2"
resourceEnvironmentEntries = resourceEnvironmentEntries.collect { s ->
  def p = s.split(';', 2)
  (p.size() == 2) ? (p[0].replaceAll(/\s+/, '') + ';' + p[1]) : s
}
customProp = customProp.collect { s ->
  def p = s.split(';', 2)
  (p.size() == 2) ? (p[0].replaceAll(/\s+/, '') + ';' + p[1]) : s
}

commonStgs.printOutput("resourceEnvironmentEntries: ${resourceEnvironmentEntries}", "G")
commonStgs.printOutput("customProp: ${customProp}", "G")
