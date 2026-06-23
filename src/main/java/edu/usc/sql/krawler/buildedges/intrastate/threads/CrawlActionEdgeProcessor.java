package edu.usc.sql.krawler.buildedges.intrastate.threads;

import edu.usc.sql.krawler.buildedges.intrastate.CrawlActionNonMutationResult;
import edu.usc.sql.krawler.buildedges.CrawlAction;
import edu.usc.sql.krawler.buildedges.intrastate.utils.CrawlEdgeExceptions;
import edu.usc.sql.krawler.buildnodes.ExtractNodesJavaScript;
import edu.usc.sql.krawler.interaction.KeyStrokeExecutor;
import edu.usc.sql.krawler.buildgraphs.CrawlTraceToState;
import edu.usc.sql.krawler.core.Results;
import edu.usc.sql.krawler.utilities.Utils;
import edu.usc.sql.krawler.utilities.Pair;
import edu.usc.sql.krawler.webproxy.GetTestSubjects;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Set;

public class CrawlActionEdgeProcessor implements Runnable {


    public CrawlAction actionToCrawl;
    //public CrawlActionMutationResult crawlActionMutationResult;
    CrawlActionEdgeProcessorPool pool;


    public CrawlActionEdgeProcessor(CrawlAction actionsToCrawl, CrawlActionEdgeProcessorPool pool) {
        this.actionToCrawl = actionsToCrawl;
        //this.crawlActionMutationResult = null;
        this.pool = pool;
    }

    @Override
    public void run() {


        String subjectName = actionToCrawl.getSubjectName();
        List<Pair<String, String>> crawlTrace = actionToCrawl.getCrawlTrace();

        String elementXpath = actionToCrawl.getElementXpath();
        String action = actionToCrawl.getAction();



        System.out.println(/*element.getXpath()*/elementXpath + "---" + action);
//        GetTestSubjects gs = new GetTestSubjects(subjectName, Config.getProxyPort(), false);//
        GetTestSubjects gs = null;

        try {
            gs = new GetTestSubjects(subjectName, pool.getProxyPort(), false);
            WebDriver refDriver = gs.getRefDriver();

            // existing body continues here

        } finally {
            if (gs != null) {
                gs.shutdownWebDriver();
            }
        }

        WebDriver refDriver = gs.getRefDriver();

        // wait for page to load
        Utils.waitForPageLoad(refDriver, 20);



        // execute crawl-trace to reach current state
        try {
            CrawlTraceToState.crawlToState(refDriver, crawlTrace);
        } catch (Exception e) {
            e.printStackTrace();
            gs.shutdownWebDriver();
            return;
        }

        Utils.waitForResourceLoad(3);

        ExtractNodesJavaScript extractNodes = new ExtractNodesJavaScript(refDriver);
        extractNodes.process();
        Set<String> stateBeforeAction = extractNodes.getNodesXpaths();


        String urlBeforeExecutingPhi = refDriver.getCurrentUrl();

        WebElement v_s = refDriver.findElement(By.xpath(/*element.getXpath()*/ elementXpath));


        // abandon thread when link's target=_blank
        if (CrawlEdgeExceptions.isNotToExecuteAction(v_s, action, pool.getUrl())) {
            System.out.println("CrawlEdgeExceptions: Thread Ended because executing non-TAB or non-SHIFTTAB action on links");
            gs.shutdownWebDriver();
            return;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        //WebElement v_s = refDriver.findElement(By.xpath(/*element.getXpath()*/ elementXpath));
        try {
            v_s.sendKeys("");                           // set focus to element
            KeyStrokeExecutor.sendKey(action, refDriver.switchTo().activeElement());                        // execute keystroke
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error interacting phi on element.");
            gs.shutdownWebDriver();
            return;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////



        Utils.waitForResourceLoad(5);

        String urlAfterExecutingPhi = refDriver.getCurrentUrl();

        // check for page unload
        if (Utils.isPageUnloaded(refDriver) || (!urlBeforeExecutingPhi.equals(urlAfterExecutingPhi) && !urlAfterExecutingPhi.contains(urlBeforeExecutingPhi + "#"))) {// !urlBeforeExecutingPhi.equals(urlAfterExecutingPhi)) {		// check window.name
            gs.shutdownWebDriver();
            Results.adddd(elementXpath + "---" + action + "---> -------" + "Page unloaded.");
            System.out.println("Page unloaded.");
            return;
        }


        ExtractNodesJavaScript extractNodes2 = new ExtractNodesJavaScript(refDriver);
        extractNodes2.process();
        Set<String> resultingStateAfterAction = extractNodes2.getNodesXpaths();


        System.out.println(/*element.getXpath()*/elementXpath + "---" + action);
        String resres = "";
        if(stateBeforeAction.equals(resultingStateAfterAction)){
            resres = "same";
            System.out.println("same");

            WebElement current_element_in_focus = refDriver.switchTo().activeElement();
            String current_element_in_focus_xpath = Utils.getAbsoluteXPath(refDriver, current_element_in_focus);


            CrawlActionNonMutationResult crawlActionNonMutationResult = new CrawlActionNonMutationResult(subjectName, crawlTrace, elementXpath, action, current_element_in_focus_xpath);
            pool.addToCrawlActionNonMutationResult(crawlActionNonMutationResult);

            System.out.println("queeeee");
            //System.exit(0);

        }else{
            resres = "different";
            System.out.println("different");

        }
        System.out.println("");
        //Results.adddd(/*element.getXpath()*/elementXpath + "---" + action + "---> -------" + resres);

        gs.shutdownWebDriver();





    }



}
