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
2. Next, the file content is inspected, with two possible outcomes:
   EDI (X12, EDIFACT, or TRADACOMS are supported) or not EDI.
3. Based on the classification of the file, metadata is extracted for
   further routing.  If the file is EDI, the standard envelope headers
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

## Metadata Extraction ##

For non-EDI files for which a preview is matched against a filter pattern,
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

## Destination Expressions ##

The route destination is a string that may include embedded expressions
of the form `${expression}`.  Any arbitrary JavaScript expression is
supported as an `expression`, but typically simple variable references
to metadata pre-loaded into the JavaScript engine's environment suffice.

The following primitives are supported:

Token                    | X12   | EDIFACT | TRADACOMS | non-EDI
-------------------------|-------|---------|-----------|--------
`sender`                 | ISA06 | UNB02:1 | STX02:1   | `(?<sender>...)`
`receiver`               | ISA08 | UNB03:1 | STX03:1   | `(?<receiver>...)`
`groupSender`            | GS02  | UNG02:1 |           | `(?<groupSender>...)`
`groupReceiver`          | GS03  | UNG03:1 |           | `(?<groupReceiver>...)`
`senderQualifier`        | ISA05 | UNB02:2 |           |
`receiverQualifier`      | ISA07 | UNB03:2 |           |
`groupSenderQualifier`   |       | UNG02:2 |           |
`groupReceiverQualifier` |       | UNG03:2 |           |
`function`               | GS01  | UNG01   |           | `(?<function>...)`
`type`                   | ST01  | UNH09:1 | MHD02     | `(?<type>...)`
`icn`                    | ISA13 | UNB05:1 | STX05:1   | `(?<icn>...)`

The following file-level primitives are also supported, but do not depend
on metadata extraction or EDI parsing:

Token               | Description
--------------------|------------
`file`              | the source filename
`base`              | the base portion of the filename (.extension removed)
`ext`               | the filename extension (including the . prefix)
`date('format')`    | the current date/time formatted with ['format'](http://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html)

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

