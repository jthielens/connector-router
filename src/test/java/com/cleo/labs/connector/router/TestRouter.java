package com.cleo.labs.connector.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import com.cleo.labs.connector.router.RoutableInputStreams.RoutableInputStream;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gwt.thirdparty.guava.common.io.CharStreams;


public class TestRouter {
    @Test
    public final void test1() {
        String json = "[{'sender'='FROM','receiver'='TO','type'='210'}]";
        Gson gson = new Gson();
        Route[] routes = gson.fromJson(json, Route[].class);
        assertEquals(1, routes.length);
        assertEquals("FROM", routes[0].sender());
    }
    static private final String[] twoonefour = new String[]{
            "ISA*00*          *00*          *02*EPES           *08*3111190000     *171020*0834*U*00401*000059772*0*P*>~"+
                "GS*QM*EPES*3111190000*20171020*0834*50630*X*004010~"+
                "ST*214*0001~"+
                "B10*4267336*51496070*EPES~"+
                "L11*0603764300*TN~"+
                "N1*SH*RJ REYNOLDS*93*WS1~"+
                "N3*200 FORUM~"+
                "N4*Rural Hall*NC*27045*USA~"+
                "N1*CN*MCLANES*93*GAATHMCL003~"+
                "N3*555 OLD HULL RD~"+
                "N4*Athens*GA*30601*USA~"+
                "N1*CN*RJ REYNOLDS*93*WS1~"+
                "N3*200 FORUM~"+
                "N4*Rural Hall*NC*27045*USA~"+
                "LX*1~"+
                "AT7*X6*NS***20171020*0112*ET~"+
                "MS1*Athens*GA~"+
                "SE*16*0001~"+
                "GE*1*50630~"+
                "IEA*1*000059772~",
            "ISA*00*          *00*          *02*EPES           *08*3111190000     *171020*0834*U*00401*000059773*0*P*>~GS*QM*EPES*3111190000*20171020*0834*50631*X*004010~ST*214*0001~B10*4267527*51496185*EPES~L11*0083052406*TN~N1*SH*RJ REYNOLDS*93*WS1~N3*200 FORUM~N4*Rural Hall*NC*27045*USA~N1*CN*SADDLE CREEK WHSE BLD# 15*93*SADDLE3010 S33801~N3*3010 SADDLECREEK RD~N4*Lakeland*FL*33801*USA~LX*1~AT7*X6*NS***20171020*0814*ET~MS1*Longwood*FL~SE*13*0001~GE*1*50631~IEA*1*000059773~",
            "ISA*00*          *00*          *02*EPES           *08*3111190000     *171020*0834*U*00401*000059774*0*P*>~GS*QM*EPES*3111190000*20171020*0834*50632*X*004010~ST*214*0001~B10*4267525*51496187*EPES~L11*0603807340*TN~N1*SH*RJ REYNOLDS*93*WS1~N3*200 FORUM~N4*Rural Hall*NC*27045*USA~N1*CN*SADDLE CREEK WHSE BLD# 15*93*SADDLE3010 S33801~N3*3010 SADDLECREEK RD~N4*Lakeland*FL*33801*USA~LX*1~AT7*X6*NS***20171020*0721*ET~MS1*Orange City*FL~SE*13*0001~GE*1*50632~IEA*1*000059774~",
            "ISA*00*          *00*          *02*EPES           *08*3111190000     *171020*0834*U*00401*000059775*0*P*>~GS*QM*EPES*3111190000*20171020*0834*50633*X*004010~ST*214*0001~B10*4267456*51496243*EPES~L11*TP101917D*TN~N1*SH*TRADEPORT WAREHOUSE*93*TNMEMTRA001~N3*5106 TRADEPORT DRIVE~N4*Memphis*TN*38141*USA~N1*CN*RJ REYNOLDS/DSC LOGISTICS*93*WS5~N3*700 N MAIN STREET~N4*Kernersville*NC*27284*USA~LX*1~AT7*X6*NS***20171020*0735*ET~MS1*West Marion*NC~SE*13*0001~GE*1*50633~IEA*1*000059775~"};
    static private final String ryder = Joiner.on("").join(new String[] {
            "ISA*00*          *00*          *02*SCAC           *01*006922827HUH1  *080903*1132*U*00401*000010067*0*P*>~",
            "GS*QM*SCAC*006922827HUH1*20080903*1132*9951*X*004010~",
            "ST*214*099510001~",
            "B10*4735103*5365205*SCAC~",
            "L11*5365205*LO~",
            "L11*01*QN~",
            "L11*392651*PO~",
            "L11*392651*PO~",
            "N1*SH*HUHTAMAKI FSBU~",
            "N3*5566 NEW VIENNA ROAD~",
            "N4*NEW VIENNA*OH*45159*US~",
            "N1*CN*HUHTAMAKI~",
            "N3*100 HIGGENSON AVE~",
            "N4*LINCOLN*RI*02865*US~",
            "LX*1~",
            "AT7***AA*NA*20080903*16000000*ET~",
            "MS1*NEW VIENNA*OH*US~",
            "AT8*G*L*6240*402~",
            "SE*17*099510001~"});
    @Test
    public final void testEDI() throws IOException {
        InputStream bis = new ByteArrayInputStream(Joiner.on("").join(twoonefour).getBytes());
        int count = 0;
        Route r = new Route().sender("EPES").receiver("3\\d+").type("214");
        Route no = new Route().sender("EPES").receiver("3\\d+").type("21");
        for (RoutableInputStream is : new RoutableInputStreams(bis, 8192)) {
            System.err.println(is.metadata().toString());
            assertEquals(twoonefour[count], CharStreams.toString(new InputStreamReader(is)));
            assertTrue(is.matches(r));
            assertFalse(is.matches(no));
            count++;
            is.close();
        }
        assertEquals(4, count);
    }
    @Test
    public final void testRyder() throws IOException {
        InputStream bis = new ByteArrayInputStream(ryder.getBytes());
        int count = 0;
        Route r = new Route().sender("SCAC").type("214");
        Route no = new Route().sender("EPES").type("214");
        for (RoutableInputStream is : new RoutableInputStreams(bis, 8192)) {
            System.err.println(is.metadata().toString());
            assertEquals(ryder, CharStreams.toString(new InputStreamReader(is)));
            assertTrue(is.matches(r));
            assertFalse(is.matches(no));
            count++;
            is.close();
        }
        assertEquals(1, count);
    }
   @Test
    public final void testPreviewLong() throws IOException {
        String notedi = "<Blink><Flim flam=\"boo\">content</Flim></Blink>";
        InputStream bis = new ByteArrayInputStream(notedi.getBytes());
        int count = 0;
        for (InputStream is : new RoutableInputStreams(bis, 8192)) {
            String ris = CharStreams.toString(new InputStreamReader(is));
            if (is instanceof PreviewInputStream) {
                PreviewInputStream pis = (PreviewInputStream) is;
                String preview = new String(pis.preview());
                assertEquals(notedi.length(), preview.length());
                assertEquals(notedi, preview);
            } else {
                fail("not a preview input stream");
            }
            assertEquals(notedi, ris);
            count++;
            is.close();
        }
        assertEquals(1, count);
    }
    @Test
    public final void testPreviewShort() throws IOException {
        String notedi = "<Blink><Flim flam=\"boo\">content</Flim></Blink>";
        InputStream bis = new ByteArrayInputStream(notedi.getBytes());
        int count = 0;
        for (InputStream is : new RoutableInputStreams(bis, 12)) {
            String ris = CharStreams.toString(new InputStreamReader(is));
            if (is instanceof PreviewInputStream) {
                PreviewInputStream pis = (PreviewInputStream) is;
                String preview = new String(pis.preview());
                assertEquals(12, preview.length());
                assertEquals(notedi.substring(0, 12), preview);
            } else {
                fail("not a preview input stream");
            }
            assertEquals(notedi, ris);
            count++;
            is.close();
        }
        assertEquals(1, count);
    }
    public final void testPreviewMatch() throws IOException {
        String s = "header is a=EPP b=XYZ c=123 with lots of other stuff";
        InputStream bis = new ByteArrayInputStream(s.getBytes());
        Route r = new Route().content(".*(?<=a=)(?<sender>\\S*).*(?<=b=)(?<receiver>\\S*).*(?<=c=)(?<type>\\S*).*")
                .sender("EPP").receiver("XYZ").type("123");
        Route no = new Route().content(".*(?<=a=)(?<sender>\\S*).*(?<=b=)(?<receiver>\\S*).*(?<=c=)(?<type>\\S*).*")
                .sender("ExP").receiver("xyZ").type("23");
        int count = 0;
        for (RoutableInputStream is : new RoutableInputStreams(bis, 8192)) {
            String ris = CharStreams.toString(new InputStreamReader(is));
            if (is instanceof PreviewInputStream) {
                PreviewInputStream pis = (PreviewInputStream) is;
                String preview = new String(pis.preview());
                assertEquals(s.length(), preview.length());
                assertEquals(s, preview);
            } else {
                fail("not a preview input stream");
            }
            assertEquals(s, ris);
            assertEquals("EPP", is.metadata().sender());
            assertEquals("XYZ", is.metadata().receiver());
            assertEquals("123", is.metadata().type());
            assertTrue(is.matches(r));
            assertFalse(is.matches(no));
            count++;
            is.close();
        }
        assertEquals(1, count);
    }
}
