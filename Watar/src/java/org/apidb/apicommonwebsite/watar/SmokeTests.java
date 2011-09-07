package org.apidb.apicommonwebsite.watar;

import java.net.URL;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.testng.annotations.*;
import static org.testng.Assert.assertEquals;


public class SmokeTests {

    static final WebClient browser;
    public String baseurl;
    public String webappname;
    
    private String geneRecordPath = "/showRecord.do?name=GeneRecordClasses.GeneRecordClass&source_id=";
    
    static {
        browser = new WebClient();
        browser.setThrowExceptionOnFailingStatusCode(false);
        browser.setJavaScriptEnabled(false);
    }

    public SmokeTests() {
        baseurl = System.getProperty("baseurl");
        webappname = System.getProperty("webappname");
    }

    /**
      * Checking HEAD response code for Home page
    **/
    @Test(description="Assert HTTP header status is 200 OK for home page.")
    public void HomePage_HttpHeaderStatusIsOK() throws Exception {
        String url = baseurl + "/" + webappname + "/";
        assertHeaderStatusMessageIsOK(url);
    }

    /**
      * Checks the HEAD response code for WsfService page as a test of Axis installation.
      * example: http://integrate.plasmodb.org/plasmo.integrate/services/WsfService
    **/
    @Test(description="Assert HTTP header status is 200 OK for WsfService url as test of Axis installation.")
    public void WsfServicePage_HttpHeaderStatusIsOK() throws Exception {
        String url = baseurl + "/" + webappname + "/services/WsfService";
        assertHeaderStatusMessageIsOK(url);
    }

    @Test(description="Assert HTTP header status is 200 OK for GeneRecord page.", 
          dataProvider="defaultGeneId",
          dependsOnMethods={"HomePage_HttpHeaderStatusIsOK"})
    public void GeneRecordPage_HttpHeaderStatusIsOK(String geneId) throws Exception {
        if (geneId == null) throw new Exception("unable to get gene id for testing");
        String url = baseurl + "/" + webappname + geneRecordPath + geneId;
        assertHeaderStatusMessageIsOK(url);
    }

    /** 
      * Returns the default value from the Gene ID Quick Search form on the front page.
    **/
    @DataProvider(name="defaultGeneId")
    private Object[][] getDefaultGeneIdFromQuickSearchForm() throws Exception {
        try {
        String url = baseurl + "/";
        WebRequest request = new WebRequest(new URL(url), HttpMethod.GET);
        HtmlPage page = (HtmlPage) browser.getPage(request);
        for (HtmlElement element : page.getElementsByTagName("input")) {
            if (element.getAttribute("name").contains("single_gene_id"))
                return new Object[][] {{ element.getAttribute("value") }};
        }
        return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        
    }

    /** 
      * Assert HEAD request returns 200 OK for the given url.
    **/
    private void assertHeaderStatusMessageIsOK(String url) throws Exception {

        try {
            WebRequest request = new WebRequest(new URL(url), HttpMethod.HEAD);
            HtmlPage page = (HtmlPage) browser.getPage(request);
            WebResponse response = page.getWebResponse();
            assertEquals(response.getStatusMessage(), "OK",  "Wrong HTTP Status for " + url + ".");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
   
    }
}