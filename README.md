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

Enabled | Filename | Content | Sender | Receiver | Group Sender | Group Receiver | Type | Destination
--------|----------|---------|--------|----------|--------------|----------------|------|------------
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
   and transaction Type are extracted.  The qalifiers for the sender
   and receiver IDs are also extracted, although they are not used for
   routing.
4. For non-EDI files, a preview buffer is loaded from the file and
   is matched against the `Content` pattern.  Rules whose `Content`
   pattern does not match the preview are eliminated from further
   consideration.  Matching rules are then inspected for named
   capture groups, which then populate a metadata object for the file
   (see Metadata Extraction below).
5. The extracted metadata is now matched against the corresponding
   patterns in the remaining rules.  Rules all of whose metadata
   patterns match the file are considered activated.
6. For each activated rule, the `Destination` template is expanded
   to calculate the destination filename.  Expressions of the form
   `${expression}` are replaced with the appropriate values (which
   can be simple metadata tokens or arbitrary JavaScript expressions
   &mdash; see Destination Expressions below).
7. The file contents are copied to the destination(s).
8. If a file matches no routes, the transfer ends in error.

### EDI Splitting ###

EDI files differ from non-EDI files not only in the way metadata is parsed.
Since EDI segments are formally structured with starting and ending segments,
it is common for files to contain separate EDI interchanges simply
concatenated in a single file.

When the router discovers an EDI file, it automatically separataes the
interchanges, and routes each as if it were a separate file.  Each
interchange is matched against the routing rules independently.  It
is possible for some interchangs to route successfully while others
fail to match any routing rules.  In this case, the overall transfer
will fail even if some interchanges were routed successully.

## Metadata Extraction ##

For non-EDI files for which a preview is matched against a filter pattern,
the filter pattern may include capture groups that describe metadata values
to extract from the content.

The following metadata tokens are supported.

## Destination Expressions ##

