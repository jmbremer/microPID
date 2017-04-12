This folder contains input and output of encoder and indexer. The XML encoder
takes a structured data file (XML) in subfolder 'xml' parses the text contained
therein, and keeps track of statistics used in a second run to Huffmann encode
the text based on word tokens rather than single characters.

The indexer then reads the compressed source file and produces the micro-pid
indexes in folder 'index' and the statistics in folder 'stats'.

The indexer stores temporary files in folder 'temp'. These files a parts of the
final index that are written to disk once the main memory used for indexing
is full. They are merged after the indexing is complete.

In folder 'dump', where index dumps go, file Dump.zip contains sample text dumps
for the tokens, terms, and data guide extracted from a number of XML sources.

In general, this delivery comes with some sample output, mostly for the smaller
XML sources described in Bremer & Gertz, 2003.