package edu.usc.sql.krawler.buildedges.interstate.threads;

import edu.usc.sql.krawler.buildedges.interstate.CrawlActionMutationResult;
import edu.usc.sql.krawler.buildedges.CrawlAction;
import edu.usc.sql.krawler.buildedges.interstate.utils.CrawlExceptions;
import edu.usc.sql.krawler.buildgraphs.CrawlTraceToState;
import edu.usc.sql.krawler.buildgraphs.UIState;
import edu.usc.sql.krawler.buildnodes.ExtractNodesJavaScript;
import edu.usc.sql.krawler.buildnodes.GetPageEntryElement;
import edu.usc.sql.krawler.interaction.KeyStrokeExecutor;
import edu.usc.sql.krawler.core.*;
import edu.usc.sql.krawler.utilities.Utils;
import edu.usc.sql.krawler.utilities.Pair;
import edu.usc.sql.krawler.webproxy.GetTestSubjects;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CrawlActionProcessor  implements Runnable {


    public CrawlAction actionToCrawl;
    //public CrawlActionMutationResult crawlActionMutationResult;
    CrawlActionProcessorPool pool;


    public CrawlActionProcessor(CrawlAction actionsToCrawl, CrawlActionProcessorPool pool) {
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

        Utils.waitForResourceLoad(1);



        // execute crawl-trace to reach current state
        try {
            CrawlTraceToState.crawlToState(refDriver, crawlTrace);
        } catch (Exception e) {
            e.printStackTrace();
            gs.shutdownWebDriver();
            return;
        }



        ExtractNodesJavaScript extractNodes = new ExtractNodesJavaScript(refDriver);
        extractNodes.process();
        Set<String> stateBeforeAction = extractNodes.getNodesXpaths();


        String urlBeforeExecutingPhi = refDriver.getCurrentUrl();




        WebElement v_s = refDriver.findElement(By.xpath(/*element.getXpath()*/ elementXpath));

        // abandon thread when link's target=_blank
        if (CrawlExceptions.isNotToExecuteAction(v_s, action, pool.getUrl())) {
            System.out.println("CrawlTraceToState: Thread Ended because target=_blank during action");
            gs.shutdownWebDriver();
            return;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        try {
            KeyStrokeExecutor.sendKey(action, v_s);                        // execute keystroke
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error interacting phi on element.");
            gs.shutdownWebDriver();
            return;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////



        Utils.waitForResourceLoad(5);

        String urlAfterExecutingPhi = refDriver.getCurrentUrl();


        // get v_0 in the new state
        GetPageEntryElement getEntry = new GetPageEntryElement(refDriver);
        getEntry.process();
        String v_entry_xpath = getEntry.getV_entry_xpath();
        String v_entry_successor_xpath = getEntry.getV_entry_successor_xpath();
        String v_entry_successor_successor_xpath = getEntry.getV_entry_successor_successor_xpath();
        String v_entry_successor_successor_successor_xpath = getEntry.getV_entry_successor_successor_successor_xpath();





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
        }else{
            resres = "different";
            System.out.println("different");

            UIState newStateAfterAction = new UIState(resultingStateAfterAction);
            newStateAfterAction.setParentStateID(this.actionToCrawl.getOriginStateOfCrawl().getStateID());
            newStateAfterAction.setStateID(Results.getUniqueUniversalStateID(newStateAfterAction));

            newStateAfterAction.setV_entry_xpath(v_entry_xpath);
            newStateAfterAction.setV_entry_successor_xpath(v_entry_successor_xpath);
//            newStateAfterAction.setV_entry_successor_successor_xpath(v_entry_successor_successor_xpath);
//            newStateAfterAction.setV_entry_successor_successor_successor_xpath(v_entry_successor_successor_successor_xpath);


            // set new UI state's crawl trace
            List<Pair<String, String>> crawlTraceNew = new ArrayList<>();
            crawlTraceNew.addAll(crawlTrace);
            Pair<String, String> actionToCrawl = new Pair<>(elementXpath, action);      // current crawling action
            crawlTraceNew.add(actionToCrawl);
            newStateAfterAction.setCrawlTrace(crawlTraceNew);


            CrawlActionMutationResult crawlActionMutationResult = new CrawlActionMutationResult(subjectName, crawlTrace, elementXpath, action, newStateAfterAction);
            crawlActionMutationResult.setOriginStateOfCrawl(this.actionToCrawl.getOriginStateOfCrawl());
            pool.addToCrawlActionMutationResult(crawlActionMutationResult);

        }
        System.out.println("");
        Results.adddd(/*element.getXpath()*/elementXpath + "---" + action + "---> -------" + resres);

        gs.shutdownWebDriver();





    }



}
