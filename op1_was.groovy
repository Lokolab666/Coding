customProp = linesProp[splitEnd..-1]
    .collect { it.trim() }
    .findAll { it }   // remove empty lines
    .collect { line ->
        if (line.contains(";")) {
            def parts = line.split(";", 2)   // split ONLY in 2 parts
            return "${parts[0]};${parts[1]}"
        } else {
            return line
        }
    }
