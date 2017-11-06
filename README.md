# README #

This is the router connector.

## TL;DR ##

The POM for this project creates a ZIP archive intended to be expanded from
the Harmony/VLTrader installation directory (`$CLEOHOME` below).

```
git clone git@github.com:jthielens/connector-router.git
mvn clean package
cp target/router-5.4.1.0-SNAPSHOT-distribution.zip $CLEOHOME
cd $CLEOHOME
unzip -o router-5.4.1.0-SNAPSHOT-distribution.zip
./Harmonyd stop
./Harmonyd start
```

When Harmony/VLTrader restarts, you will see a new `Template` in the host tree
under `Connections` > `Generic` > `Generic ROUTER`.  Select `Clone and Activate`
and a new `ROUTER` connection (host) will appear on the `Active` tab.

The new `ROUTER` needs to have one or more routes defined.  From the `ROUTER` tab, click the `...` on the right of the `Routing Table` line.  Enter `214` in the
`Type` column and `local/root/${base}.${icn}${ext}` in the `Destination` column.

Create a sample EDI file in `$CLEOHOME/outbox/test.edi` from the content below:

```
ISA*00*          *00*          *02*SCAC           *01*006922827HUH1  *080903*1132*U*00401*000010067*0*P*>~
GS*QM*SCAC*006922827HUH1*20080903*1132*9951*X*004010~
ST*214*099510001~
B10*4735103*5365205*SCAC~
L11*5365205*LO~
L11*01*QN~
L11*392651*PO~
L11*392651*PO~
N1*SH*HUHTAMAKI FSBU~
N3*5566 NEW VIENNA ROAD~
N4*NEW VIENNA*OH*45159*US~
N1*CN*HUHTAMAKI~
N3*100 HIGGENSON AVE~
N4*LINCOLN*RI*02865*US~
LX*1~
AT7***AA*NA*20080903*16000000*ET~
MS1*NEW VIENNA*OH*US~
AT8*G*L*6240*402~
SE*17*099510001~
```

(Note: this sample is drawn from Ryder's EDI documentation [here](https://ryder.com/-/media/ryder/ryder-global/carriers/carrier_214.pdf)).

Change the default `<send>` action to read `PUT test.edi` (instead of
`PUT -DEL *`) and run the action.  You will find a routed file
`test.000010067.edi` as output in `local/root`.

## Routing ##

The routing table is an unordered list of matching rules that specify
the routing destination.  All matching columns are [regular expressions]
(http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html),
while the `Destination` is an expression template used to calculate the
destination location.  In general, if a column is left blank in a routing
table entry, then it always matches the associated criterion.

Enabled | Filename | Content | Sender | Receiver | Group Sender | Group Receiver | Function | Type | Destination
--------|----------|---------|--------|----------|--------------|----------------|----------|------|------------
&nbsp;  | 

The routing process proceeds in stages:

1. First, the filename is matched against the `Filename` rules, and any
   rules not matching a specified pattern are eliminated from further
   matching.
2. Next, the file content is inspected, with three possible outcomes:
   EDI (X12, EDIFACT, or TRADACOMS are supported), HL7 or not EDI.
3. Based on the classification of the file, metadata is extracted for
   further routing.  If the file is EDI or HL7, the standard envelope headers
   are parsed and the Sender, Receiver, Group Sender, Group Receiver,
   Function, and transaction Type are extracted.  The qualifiers for the sender
   and receiver IDs are also extracted, although they are not used for
   routing.  Rules with a `Content` pattern are eliminated from
   further consideration for EDI files.
4. For non-EDI files, a preview buffer is loaded from the file and
   is matched against the `Content` pattern.  Rules whose `Content`
   pattern is empty or does not match the preview are eliminated from further
   consideration.  Matching rules are then inspected for named
   capture groups, which then populate a metadata object for the file
   (see [Metadata Extraction](#metadata-extraction) below).
5. The extracted metadata is now matched against the corresponding
   patterns in the remaining rules.  Rules all of whose metadata
   patterns match the file are considered activated.
6. For each activated rule, the `Destination` template is expanded
   to calculate the destination filename.  Expressions of the form
   `${expression}` are replaced with the appropriate values (which
   can be simple metadata tokens or arbitrary JavaScript expressions
   &mdash; see [Destination Expressions](#destination-expressions) below).
7. The file contents are copied to the destination(s).
8. If a file matches no routes, but an error destionation is configured,
   the file is copied to the error destination.  Note that metadata
   tokens can be expanded in the error destination string.
9. If a file matches no routes and no error destionation is configured,
   the transfer ends in error.

### EDI Splitting ###

EDI files differ from non-EDI files not only in the way metadata is parsed.
Since EDI segments are formally structured with starting and ending segments,
it is common for files to contain separate EDI interchanges simply
concatenated in a single file.

When the router discovers an EDI file, it automatically separataes the
interchanges, and routes each as if it were a separate file.  Each
interchange is matched against the routing rules independently.  It
is possible for some interchanges to route successfully while others
fail to match any routing rules.  In this case, the overall transfer
will fail even if some interchanges were routed successully.

### Non-EDI Preview Size ###

Metadata extraction for non-EDI files requires that first a preview of
the file content is read from the source file for matching against the
`Content` pattern.  The `Preview Size` is configured as a property of
the Router Connector, and defaults to `8k`.  You may enter values for
the `Preview Size` using _human readable_ suffixes for convenience.  In
the table below, _nnn_ represents an arbitrary integer value.

Preview Size      | Result
------------------|-------
_nnn_             | up to _nnn_ bytes are loaded for preview
<em>nnn</em>k or <em>nnn</em>kb | _nnn_ [kibibytes](https://en.wikipedia.org/wiki/Kibibyte) (&times; 1024) bytes are loaded
<em>nnn</em>m or <em>nnn</em>mb | _nnn_ [mebibytes](https://en.wikipedia.org/wiki/Mebibyte) (&times; 1024&sup2;) bytes are loaded

The preview buffer is held entirely in memory, so be mindful of practical
resource limits when configuring the preview size.

### HL7 Handling ###

HL7 is a bit different in structure from the three supported EDI standards.
When an HL7 file is detected, the `MSH` segment is parsed and some of its
fields are mapped to the routing table entries as described here.  Unlike
the EDI formats, HL7 files are not split, so the file is not inspected
for subsequent `MSH` segments.

Item   | Type | Length | Description | Mapped To
-------|------|--------|-------------|----------
MSH-01 | ST   | 1      | Field separator         |
MSH-02 | ST   | 4      | Encoding characters     |
MSH-03 | HD   | 180    | Sending Application     | `groupSender` and `groupSenderQualifier`
MSH-04 | HD   | 180    | Sending Facility        | `sender` and `senderQualifier`
MSH-05 | HD   | 180    | Receiving Application   | `groupReceiver` and `groupReceiverQualifier`
MSH-06 | HD   | 180    | Receiving Facility      | `receiver` and `receiverQualifier`
MSH-07 | TS   | 26     | Date/Time Of Message    |
MSH-08 | ST   | 40     | Security                |
MSH-09 | ST   | 7      | Message Type            | `type` and `function`
MSH-10 | ST   | 20     | Message Control ID      | `icn`
MSH-11 | PT   | 3      | Processing ID           |
MSH-12 | ID   | 8      | Version ID              |
MSH-13 | NM   | 15     | Sequence Number         |
MSH-14 | ST   | 180    | Continuation Pointer    |
MSH-15 | ID   | 2      | Accept Acknowledgment Type      |
MSH-16 | ID   | 2      | Application Acknowledgment Type |
MSH-17 | ID   | 2      | Country Code            |
MSH-18 | ID   | 6      | Character Set           |
MSH-19 | CE   | 60     | Principal Language Of Message   |

The HL7 Message Type comprises a message type ID and trigger event ID.
The message type ID is mapped to the EDI `type` and the trigger event
ID is mapped to the `function`.  For example, a message with a MSH-09
message type of `ADT^A04` would be parsed into a `type` of `ADT` (Admit
Discharge Transfer) with a `function` of `A04` (Register a patient).

The sender and receiver HL7 types are `HD` (Hierarchic Designator), which
may have up to three sub-composites, with the first part optional.  If more
than two parts are found, the first part is discarded, the second mapped
to the appropriate identifier, and the third is mapped to the associated
qualified.  If two parts are found, the first and second are mapped to
the identifier and its qualifier.  If there are no sub-composites, the
entire value is mapped to the identifier and the qualifier is blank.

HL7 detection depends on successful parsing of the `MSH` segment into
at least 12 parts (up to `MSH-12` "Version ID") and a Version ID that
starts with `2.`.

## Metadata Extraction ##

For non-EDI files for which a preview is matched against a `Content` filter pattern,
the filter pattern may include capture groups that describe metadata values
to extract from the content.

Named capture groups are expressed in a pattern as `(?<token>pattern)`
where `token` is one of the supported token names from the list below
and `pattern` is a regular expression matching the content to be
extracted.  For example, if a route is matching an XML document
that may contain embedded elements `<sender>some value</sender>`
or `<receiver>some value</receiver>`, the following expression will
match and extract the `some value` portions, irrespective of the
order in which the `<sender>` and `<receiver>` elements appear in the
document:

```
(?:.*?(?:<sender>(?<sender>[^<]*)</sender>|<receiver>(?<receiver>[^<]*)</receiver>))*.*
```

The anatomy of this regular expression comprises:

* an outer framework of `(?:.*?thing)*.*` (keep in mind that `(?:pattern)` is
  a non-capturing grouping expression) which means a sequence of interesting
  `thing`s, separated by padding text `.*?`, followed by arbitrary padding text
  `.*`.
* a definition of `thing` as a choice between alternatives `(?:a|b)` (this
  could be extended to `(?:a|b|c|...)` as needed).
* each alternative is an XML fragment `<tag>value</tag>`.
* each `value` is captured as a token `(?<token>...)` and contains arbitrary
  text up to the closing tag `[^<]*`.

The following metadata tokens are supported (the tokens are case sensitive):

Expression                | Description
--------------------------|------------
`file`                    | the source filename
`base`                    | the base portion of the filename (.extension removed)
`ext`                     | the filename extension (including the . prefix)
`sender`                  | the sender metadata
`receiver`                | the receiver metadata
`groupSender`             | the functional group sender metadata
`groupReceiver`           | the functional group receiver metadata
`function`                | the functional group identification
`type`                    | the transaction type
`icn`                     | the interchange control number

### Named `Content` Patterns ###

Often when routing based on parsed content a single metadata extraction
and matching pattern will be used to select between multiple destinations
based on the extracted values (since EDI and HL7 have well-known fixed
schemas, the routing table naturally supports this use case).

Since the routing table combines the metadata extraction pattern
in the `Content` column with metadata matching patterns in the `Sender`,
`Receiver`, etc. columns, the same `Content` metadata extraction
pattern might have to be repeated for each matching pattern and
destination:

Content                                | Sender | Destination
---------------------------------------|--------|------------
`.*?<token>(?<sender>[^<]*)</token>.*` | Acme   | /files/inbound/acmebrands
`.*?<token>(?<sender>[^<]*)</token>.*` | Bigco  | /files/inbound/bigfoods

To better support this kind of routing, the router connector allows
`Content` patterns to be named and reused.  A name starts with a letter
a-z or A-Z, followed by any number of letters or digits a-z, A-Z, or 0-9.
If a `Content` pattern starts with `name:`, any text following the `name:`
will be parsed as the matching pattern and the metadata results will be
stored and associated with `name`.  Any subsequence references in the same
routing table to `name:` without a pattern will re-use the same metadata.

Content                                       | Sender | Destination
----------------------------------------------|--------|------------
`sender:.*?<token>(?<sender>[^<]*)</token>.*` | Acme   | /files/inbound/acmebrands
`sender:`                                     | Bigco  | /files/inbound/bigfoods

Since the `Destination` is technically optional (although no file will be delivered
to an empty destination), the table can also designed to separate pattern definitions
from references:

Content                                       | Sender | Destination
----------------------------------------------|--------|------------
`sender:.*?<token>(?<sender>[^<]*)</token>.*` |        |
`sender:`                                     | Acme   | /files/inbound/acmebrands
`sender:`                                     | Bigco  | /files/inbound/bigfoods

Often it is convenient to separate the metadata extraction patterns into
multiple patterns, for example, when tokens may appear in any order.  The
named patterns can be combined to help with this:

Content                                     | Sender | Receiver | Destination
--------------------------------------------|--------|----------|------------
`sender:.*?<from>(?<sender>[^<]*)</from>.*` |        |          |
`receiver:.*?<to>(?<receiver>[^<]*)</to>.*` |        |          |
`sender,receiver:`                          | Usinc  | Acme     | /files/inbound/acmebrands
`sender,receiver:`                          | Usinc  | Bigco    | /files/inbound/bigfoods

Note:

>* If a pattern appears after a name list `name1,name2:pattern`, the `pattern` replaces
>any previous pattern for `name1`, but `name2` will reference a previous pattern.
>
> * Whitespace may appear around the `,` or before the `:`, but whitespace after the
> `:` is considered part of the pattern.
> 
> * To avoid having a pattern starting with `name:` being interpreted as a name
> instead of part of the pattern, surround it with a non-capturing group `(?:name:)`. 

## Destination Expressions ##

The route destination is a string that may include embedded expressions
of the form `${expression}`.  Any arbitrary JavaScript expression is
supported as an `expression`, but typically simple variable references
to metadata pre-loaded into the JavaScript engine's environment suffice.

The following primitives are supported:

Token                    | X12   | EDIFACT | TRADACOMS | HL7      | non-EDI
-------------------------|-------|---------|-----------|----------|--------
`sender`                 | ISA06 | UNB02:1 | STX02:1   | MSH-04.2 | `(?<sender>...)`
`receiver`               | ISA08 | UNB03:1 | STX03:1   | MSH-06.2 | `(?<receiver>...)`
`groupSender`            | GS02  | UNG02:1 |           |          | `(?<groupSender>...)`
`groupReceiver`          | GS03  | UNG03:1 |           |          | `(?<groupReceiver>...)`
`senderQualifier`        | ISA05 | UNB02:2 |           | MSH-04.3 |
`receiverQualifier`      | ISA07 | UNB03:2 |           | MSH-06.3 |
`groupSenderQualifier`   |       | UNG02:2 |           |          |
`groupReceiverQualifier` |       | UNG03:2 |           |          |
`function`               | GS01  | UNG01   |           | MSH-09.2 | `(?<function>...)`
`type`                   | ST01  | UNH09:1 | MHD02     | MSH-09.1 | `(?<type>...)`
`icn`                    | ISA13 | UNB05:1 | STX05:1   | MSH-10   | `(?<icn>...)`

The following file-level primitives are also supported, but do not depend
on metadata extraction or EDI parsing:

Token               | Description
--------------------|------------
`file`              | the source filename
`base`              | the base portion of the filename (.extension removed)
`ext`               | the filename extension (including the . prefix)
`date('format')`    | the current date/time formatted with ['format'](http://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html)
`unique`            | a uniqueness token, either empty or `.n` where `n` starts at `1` and counts up as needed

For example, the destination:

```
/path/${file}.${icn}${ext}
```

will expand the three embedded metadata variable references from
the destination filename and file content parsing.
This could also be expressed using JavaScript as:

```
/path/${file+'.'+icn+ext}
```

As a performance optimization, any JavaScript expression of the form
`${token}` for one of the tokens above is expanded without invoking
the JavaScript engine.  Also, the primitives `${date('string')}` or
`${date("string")}` are similarly expanded without invoking JavaScript.
This means that although the two examples above produce the same
result, the first one executes much more quickly.

Note that URI destinations are supported, so a destination of the form:

```
scheme:connection/path
```

can be used to route a file through a URI.

### Unique Filenames ###

If the `-UNIque` option is used with the connector `PUT` command, the connector will test expanded
destination expressions for existence in order to prevent overwriting an existing file.  If the
expanded destination exists, the connector begins inserting a uniqueness token into the filename
until it can find an unclaimed filename.  Uniqueness tokens include a leading `.` followed by
the counter, e.g. `.1`, `.2`, etc.

The placement of the uniqueness token can be explicitly controlled by placing the `${unique}`
token in the destination expression (or some JavaScript expression deriving a value from
the `unique` string variable, which will either be empty or `.n` as described).

If a destination expression does not explicitly include `${unique}`, the uniqueness token
will automatically be inserted just before the filename extension, or at the end of the
filename if there is no extension, e.g.

* `filename.txt` &rarr; `filename.1.txt` &rarr; `filename.2.txt` &hellip;
* `filename` &rarr; `filename.1` &rarr; `filename.2` &hellip;

This calculation of unique filenames depends on route processing.  In the context
of a `PUT` command, this is easy to control.  But when the use of the connector
is most convenient through a URI `router:host` and `LCOPY` commands, the `-UNI`
flag to `LCOPY` will not work.  In this case, set the `Force Unique` property on
the connector.