package edu.usc.sql.krawler.core;


import edu.usc.sql.krawler.buildedges.CrawlAction;
import edu.usc.sql.krawler.buildedges.SimpleBuildDirectional;
import edu.usc.sql.krawler.buildedges.interstate.CrawlActionMutationResult;
import edu.usc.sql.krawler.buildedges.interstate.threads.CrawlActionProcessorPool;
import edu.usc.sql.krawler.buildedges.intrastate.CrawlActionNonMutationResult;
import edu.usc.sql.krawler.buildedges.intrastate.threads.CrawlActionEdgeProcessorPool;
import edu.usc.sql.krawler.buildgraphs.GetCrawlActionsForUIState;
import edu.usc.sql.krawler.buildgraphs.Hop;
import edu.usc.sql.krawler.buildgraphs.HopForwardDeeper;
import edu.usc.sql.krawler.buildgraphs.UIState;
import edu.usc.sql.krawler.buildnodes.ExtractNodesJavaScript;
import edu.usc.sql.krawler.buildnodes.GetPageEntryElement;
import edu.usc.sql.krawler.graphs.UIGraph;
import edu.usc.sql.krawler.graphs.UIGraphEdge;
import edu.usc.sql.krawler.graphs.UIGraphNode;
import edu.usc.sql.krawler.graphs.UIGraphState;
import edu.usc.sql.krawler.utilities.Utils;
import edu.usc.sql.krawler.utilities.Pair;
import edu.usc.sql.krawler.webproxy.GetTestSubjects;
import lombok.NoArgsConstructor;
import org.openqa.selenium.WebDriver;

import java.util.*;

@NoArgsConstructor
public class Construct_Krawler {

  String subjectName;
  WebDriver refDriver;
  String url;
  int proxyPort;
  boolean injection;
  GetTestSubjects gs;
  String screenModalityType = "";
  int viewPort_x = 0;
  int viewPort_y = 0;
  int zoomPercentage = 0;

  //String[] keysToCrawl;
  Set<String> keysToCrawl;
  int numberOfConcurrentWebdrivers;

  public Construct_Krawler(GetTestSubjects gs, /*RunnerTracker coct,*/ String screenModalityType, int viewPort_x,
                           int viewPort_y, int zoomPercentage, Set<String> keysToCrawl, int numberOfConcurrentWebdrivers) {
    this.subjectName = gs.getSubjectName();
    this.refDriver = gs.getRefDriver();
    this.url = gs.getUrl();
    this.proxyPort = gs.getProxyPort();
    this.injection = gs.isInjection();
    this.gs = gs;
    this.screenModalityType = screenModalityType;
    this.viewPort_x = viewPort_x;
    this.viewPort_y = viewPort_y;
    this.zoomPercentage = zoomPercentage;

    this.keysToCrawl = keysToCrawl;
    this.numberOfConcurrentWebdrivers = numberOfConcurrentWebdrivers;
  }

  public Set<String> getInitialUIState(){
    ExtractNodesJavaScript extractNodes = new ExtractNodesJavaScript(refDriver);
    extractNodes.process();
    Set<String> nodesXpaths = extractNodes.getNodesXpaths();


//    Set<String> nodesXpaths2 = new HashSet<>();
//    for(String s : nodesXpaths){
//      if(s.equals("/html[1]/body[1]/main[1]/nav[2]/div[1]/button[1]")){
//        nodesXpaths2.add(s);
//      }
//    }
//    nodesXpaths = nodesXpaths2;



    System.out.println(nodesXpaths.size());

    return nodesXpaths;
  }

  public Set<CrawlAction> getInitialDeeperCrawlActions(UIState initialUIState){
    Set<String> nodesXpaths = initialUIState.getNodesXpaths();
    Set<CrawlAction> hop0CrawlAction = new HashSet<>();
    for(String nodesXpath:nodesXpaths){
      CrawlAction ca = null;
      if(keysToCrawl.contains("ENTER")){
        ca = new CrawlAction(subjectName, 0, new ArrayList<>(), nodesXpath, "ENTER");
        ca.setOriginStateOfCrawl(initialUIState);
        hop0CrawlAction.add(ca);
      } else {
//        for(String key:keysToCrawl){
//          if(key.equalsIgnoreCase("TAB") || key.equalsIgnoreCase("SHIFTTAB")){  continue;  }
//          ca = new CrawlAction(subjectName, 0, new ArrayList<>(), nodesXpath, key);
//        }

      }


    }

    return hop0CrawlAction;
  }

  public Set<CrawlActionMutationResult> getCrawlActionResults(Hop hop){
    Set<CrawlAction> hopCrawlAction = hop.getHopCrawlAction();

    Set<CrawlActionMutationResult> hopCrawlActionMutationResult = new HashSet<>();
    CrawlActionProcessorPool cappool = new CrawlActionProcessorPool(hopCrawlAction, numberOfConcurrentWebdrivers, url, proxyPort);
    cappool.start();
    if (cappool.isFinishedProcessing()) {
      hopCrawlActionMutationResult.addAll(cappool.getCrawlActionMutationResults());
    }

    return hopCrawlActionMutationResult;
  }

  public void processCrawlActionResults(Hop hop){
    Set<CrawlActionMutationResult> hopiCrawlActionMutationResult = hop.getHopCrawlActionMutationResult();

    for(CrawlActionMutationResult camr : hopiCrawlActionMutationResult){
      UIState resultingState = camr.getResultingState();
      Results.addToVisitedStates(resultingState);

      System.out.println(camr);

      // create state transition
      UIState originalState = camr.getOriginStateOfCrawl();

      String v_s_xpath = camr.getElementXpath();
      String action = camr.getAction();
      int nextStateID = resultingState.getStateID();
      String v_t_xpath = resultingState.getV_entry_xpath();
      String v_t_successor_xpath = resultingState.getV_entry_successor_xpath();

      Pair<Pair<Pair<String, String>, Integer>, Pair<String, String>> nextStateTransition = new Pair<>(new Pair<>(new Pair<>(v_s_xpath, action), nextStateID), new Pair<>(v_t_xpath, v_t_successor_xpath));
      originalState.addToNextStateTransitionList(nextStateTransition);

    }
//    if(hop.getHop()==1){
//      System.out.println(hopiCrawlActionMutationResult.size());
//      System.exit(0);
//    }


    System.out.println("getVisitedStates");
    System.out.println(Results.getVisitedStates().size());
    System.out.println(Results.getVisitedStates());
  }

  public void process() {
    Utils.waitForResourceLoad(1);



    GetPageEntryElement getEntry = new GetPageEntryElement(refDriver);
    getEntry.process();



    int maxNumberOfHopsInCrawlDepth = 1;//3;

    Set<String> nodesXpaths = getInitialUIState();
    UIState initialUIState = new UIState(nodesXpaths);
    initialUIState.setParentStateID(-1);
    initialUIState.setStateID(Results.getUniqueUniversalStateID(initialUIState));
    initialUIState.setCrawlTrace(new ArrayList<>());

    initialUIState.setV_entry_xpath(getEntry.getV_entry_xpath());
    initialUIState.setV_entry_successor_xpath(getEntry.getV_entry_successor_xpath());
//    initialUIState.setV_entry_successor_successor_xpath(getEntry.getV_entry_successor_successor_xpath());
//    initialUIState.setV_entry_successor_successor_successor_xpath(getEntry.getV_entry_successor_successor_successor_xpath());



    Results.addToVisitedStates(initialUIState);




/*
    Hop hop0 = new Hop(0);
    hop0.setHopCrawlAction(getInitialDeeperCrawlActions(initialUIState));


    Hop hopCurrent = hop0;



      Hop hopi = hopCurrent;
      // crawl the hop0 actions to get the crawl action results (those that caused a UI mutation, not page unload or same UI)
      Set<CrawlActionMutationResult> hopiCrawlActionMutationResult = getCrawlActionResults(hopi);

      hopi.setHopCrawlActionMutationResult(hopiCrawlActionMutationResult);
      processCrawlActionResults(hopi);
*/









    Hop hop0 = new Hop(0);
    hop0.setHopCrawlAction(getInitialDeeperCrawlActions(initialUIState));


    Hop hopCurrent = hop0;


    for (int i = 0; i < maxNumberOfHopsInCrawlDepth; i++) {
      Hop hopi = hopCurrent;
      // crawl the hop0 actions to get the crawl action results (those that caused a UI mutation, not page unload or same UI)
      Set<CrawlActionMutationResult> hopiCrawlActionMutationResult = getCrawlActionResults(hopi);

      hopi.setHopCrawlActionMutationResult(hopiCrawlActionMutationResult);
      processCrawlActionResults(hopi);

      // set up next hop based on previous hop data
      // create the crawl actions for next hop based on the crawl action results from current hop
      // *while ignored already visited state*
      Hop hopiPlus1 = HopForwardDeeper.process(hopi);
      System.out.println("taco " + i + " " + hopi.getHop() + " " + hopiPlus1.getHop());
      System.out.println(hopiPlus1.getHop());
      hopiPlus1.setHopCrawlAction(hopiPlus1.getHopCrawlAction());
      hopCurrent = hopiPlus1;

    }





//    Set<String> keysToCrawl = new HashSet<>();
//    keysToCrawl.add("TAB");
//    keysToCrawl.add("SHIFTTAB");
//    keysToCrawl.add("UP");
//    keysToCrawl.add("DOWN");
//    keysToCrawl.add("LEFT");
//    keysToCrawl.add("RIGHT");
//    keysToCrawl.add("SPACE");



    System.out.println("states");
    //Set<CrawlAction> actionsToCrawlForEdges = new HashSet<>();
    Set<UIState> states = Results.getVisitedStates();
    System.out.println(states.size());
    for(UIState s : states){
      System.out.println(s);
    }
    //System.exit(0);

    // Prompt the user for input
//    Scanner scanner = new Scanner(System.in);
//    System.out.print("getVisitedStates");
//    scanner.nextLine();


    for (UIState state : states) {
      //PeriodicUpdate pu = new PeriodicUpdate(state.getStateID());


      UIGraph uiState = new UIGraph();
      uiState.setNodes(new HashSet<>());
      uiState.setEdges(new HashSet<>());



      UIGraphState uiGraphState = new UIGraphState(uiState);
      uiGraphState.setParentUniversalStateID(state.getParentStateID());
      uiGraphState.setUniversalStateID(state.getStateID());

      uiGraphState.setNextStateTransitionList(state.getNextStateTransitionList());

      UIGraphNode v_entry = new UIGraphNode(state.getV_entry_xpath());
      UIGraphNode v_entry_successor = new UIGraphNode(state.getV_entry_successor_xpath());
      uiGraphState.setV_entry(v_entry);
      uiGraphState.setV_entry_successor(v_entry_successor);

      Results.addToExploredDOMStateSet_size(uiGraphState);
    }


    System.out.print("yo what ");
    //System.exit(0);


    Set<String> keysToCrawlForEdges = new HashSet<>(keysToCrawl);
    keysToCrawlForEdges.remove("TAB");
    keysToCrawlForEdges.remove("SHIFTTAB");
    keysToCrawlForEdges.remove("ENTER");

    ///////// VERSION 1
    for (UIState state : states) {
      Set<CrawlAction> actionsToCrawlForEdgesForUIState = new HashSet<>();


      Set<CrawlAction> ac = GetCrawlActionsForUIState.process(subjectName, state, keysToCrawlForEdges);
      actionsToCrawlForEdgesForUIState.addAll(ac);

//      actionsToCrawlForEdgesForUIState.addAll(
//              ac.stream()
//                      .limit(5)
//                      .collect(Collectors.toSet())
//      );


      System.out.println("actionsToCrawlForEdgesForUIState");
      System.out.println(actionsToCrawlForEdgesForUIState.size());
      for(CrawlAction caa: actionsToCrawlForEdgesForUIState){
        System.out.println(caa);
      }

      System.out.print("actionsToCrawlForEdgesForUIState");
//      scanner.nextLine();


/*
*/
      UIGraph uiState = new UIGraph();
      Set<UIGraphNode> nodes = new HashSet<>();
      Set<UIGraphEdge> edges = new HashSet<>();


      // first do directional (tab and shifttab)
      doDirectionCrawl("TAB", state, nodes, edges);
      doDirectionCrawl("SHIFTTAB", state, nodes, edges);




      // next do other keys
      Set<CrawlActionNonMutationResult> edgeCrawlActionNonMutationResult = new HashSet<>();
      CrawlActionEdgeProcessorPool cappool_edge = new CrawlActionEdgeProcessorPool(actionsToCrawlForEdgesForUIState, numberOfConcurrentWebdrivers, url, proxyPort);
      cappool_edge.start();
      if (cappool_edge.isFinishedProcessing()) {
        edgeCrawlActionNonMutationResult.addAll(cappool_edge.getCrawlActionNonMutationResults());
      }

      System.out.println("kfc" + edgeCrawlActionNonMutationResult.size());
      for(CrawlActionNonMutationResult canr : edgeCrawlActionNonMutationResult){
        System.out.println(canr);
      }
      System.out.print("edgeCrawlActionNonMutationResult");
//      scanner.nextLine();


      //System.exit(0);

      for(CrawlActionNonMutationResult canr : edgeCrawlActionNonMutationResult){
        String v_s_xpath = canr.getElementXpath();
        String v_t_xpath = canr.getResultingFocus();
        UIGraphNode v_s = new UIGraphNode(v_s_xpath);
        UIGraphNode v_t = new UIGraphNode(v_t_xpath);
        UIGraphEdge e = new UIGraphEdge(v_s, v_t, canr.getAction());

        nodes.add(v_s);
        nodes.add(v_t);
        edges.add(e);



      }
      uiState.setNodes(nodes);
      uiState.setEdges(edges);

      for(UIGraphEdge e: edges){
        System.out.println(e);
      }
      //System.exit(0);

      UIGraphState uiGraphState = new UIGraphState(uiState);
      uiGraphState.setParentUniversalStateID(state.getParentStateID());
      uiGraphState.setUniversalStateID(state.getStateID());

      uiGraphState.setNextStateTransitionList(state.getNextStateTransitionList());

      UIGraphNode v_entry = new UIGraphNode(state.getV_entry_xpath());
      UIGraphNode v_entry_successor = new UIGraphNode(state.getV_entry_successor_xpath());
      uiGraphState.setV_entry(v_entry);
      uiGraphState.setV_entry_successor(v_entry_successor);

      Results.addToExploredDOMStateSet_size(uiGraphState);


/*
      UIGraphState uiGraphState = new UIGraphState(uiState);
      uiGraphState.setParentUniversalStateID(state.getParentStateID());
      uiGraphState.setUniversalStateID(state.getStateID());

      uiGraphState.setNextStateTransitionList(state.getNextStateTransitionList());

      UIGraphNode v_entry = new UIGraphNode(state.getV_entry_xpath());
      UIGraphNode v_entry_successor = new UIGraphNode(state.getV_entry_successor_xpath());
      uiGraphState.setV_entry(v_entry);
      uiGraphState.setV_entry_successor(v_entry_successor);

      Results.addToExploredDOMStateSet_size(uiGraphState);

 */

    } /////////// END VERSION 1


/*
    ///////// VERSION 2
    for (UIState state : states) {

      UIGraph uiState = new UIGraph();
      Set<UIGraphNode> nodes = new HashSet<>();
      Set<UIGraphEdge> edges = new HashSet<>();


      List<Pair<String, String>> crawlTrace = state.getCrawlTrace();
      int numOfIterations = state.getNodesXpaths().size() * 2;

      SimpleBuildDirectional sbd_tab = new SimpleBuildDirectional(subjectName, crawlTrace, "TAB", numOfIterations);
      sbd_tab.process();
      nodes.addAll(sbd_tab.getNodes());
      edges.addAll(sbd_tab.getEdges());

      SimpleBuildDirectional sbd_shifttab = new SimpleBuildDirectional(subjectName, crawlTrace, "SHIFTTAB", numOfIterations);
      sbd_shifttab.process();
      nodes.addAll(sbd_shifttab.getNodes());
      edges.addAll(sbd_shifttab.getEdges());





      uiState.setNodes(nodes);
      uiState.setEdges(edges);

      UIGraphState uiGraphState = new UIGraphState(uiState);
      uiGraphState.setParentUniversalStateID(state.getParentStateID());
      uiGraphState.setUniversalStateID(state.getStateID());

      uiGraphState.setNextStateTransitionList(state.getNextStateTransitionList());

      UIGraphNode v_entry = new UIGraphNode(state.getV_entry_xpath());
      UIGraphNode v_entry_successor = new UIGraphNode(state.getV_entry_successor_xpath());
      uiGraphState.setV_entry(v_entry);
      uiGraphState.setV_entry_successor(v_entry_successor);

      Results.addToExploredDOMStateSet_size(uiGraphState);

    } /////////// END VERSION 2
*/




  }

  public void doDirectionCrawl(String direction, UIState state, Set<UIGraphNode> nodes, Set<UIGraphEdge> edges){

    List<Pair<String, String>> crawlTrace = state.getCrawlTrace();
    int numOfIterations = state.getNodesXpaths().size() * 2;

    SimpleBuildDirectional sbd = new SimpleBuildDirectional(subjectName, crawlTrace, direction, numOfIterations, proxyPort);
    sbd.process();
    nodes.addAll(sbd.getNodes());
    edges.addAll(sbd.getEdges());



  }

}
