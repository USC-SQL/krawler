package edu.usc.sql.krawler;

//import edu.usc.sql.krawler.io.WriteExecutionTimeToCSV;
import edu.usc.sql.krawler.graphs.UIGraphState;
import edu.usc.sql.krawler.io.WriteUIGraphToJSON;
import edu.usc.sql.krawler.core.Construct_Krawler;
import edu.usc.sql.krawler.core.Results;
import edu.usc.sql.krawler.utilities.Config;
import edu.usc.sql.krawler.utilities.Utils;
import edu.usc.sql.krawler.utilities.KeysToCrawl;
import edu.usc.sql.krawler.webproxy.GetTestSubjects;
import edu.usc.sql.krawler.webproxy.MitmproxyJava;

import java.io.File;
import java.util.*;

public class RunApproachKrawler {

  public static String mitmproxyPath;
  public static MitmproxyJava mitmproxy;
  public static String cachePath;
  public static Integer proxyPort;

  public static void main(String[] args) {

    //Config.applyConfig();

//    HashMap<String, String> subjectsToRunList = prepareData();
//    for (String subjectUniqueName : subjectsToRunList.keySet()) {
    List<String> subjectsToRunList = prepareData2();

    for (String subjectUniqueName : subjectsToRunList) {
      String subjectOutputDir =
              Config.getOutputBaseDir()
                      + File.separator
                      + "KERKrawler"
                      + File.separator
                      + subjectUniqueName
                      + File.separator;

      long start = System.currentTimeMillis();
      boolean completedSuccessfully = false;

      try {
        startMitmproxy(subjectUniqueName);

        run(subjectUniqueName, 1280, 1024, 100, proxyPort);
        write(subjectOutputDir, subjectUniqueName);

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.println("timeElapsed: " + timeElapsed);

        System.out.println("RunApproachKRFG: Done");

        completedSuccessfully = true;

      } catch (Exception e) {
        System.err.println("RunApproachKrawler failed for subject: " + subjectUniqueName);
        e.printStackTrace();

      } finally {
        try {
          stopMitmproxy();
        } catch (Exception e) {
          System.err.println("RunApproachKrawler: stopMitmproxy failed for subject: " + subjectUniqueName);
          e.printStackTrace();
        }

        try {
          hardReset();
        } catch (Exception e) {
          System.err.println("RunApproachKrawler: hardReset failed for subject: " + subjectUniqueName);
          e.printStackTrace();
        }

        if (completedSuccessfully) {
          try {
            //markSubjectAsSuccessfullyProcessed(subjectUniqueName);
          } catch (Exception e) {
            System.err.println("RunApproachKrawler: failed to mark subject as successfully processed: " + subjectUniqueName);
            e.printStackTrace();
          }
        }
      }
    }

    System.out.println("RunApproachKNFG: Reached end of Krawler");
    //System.exit(0);
  }

  public static void run(
          String SUBJECT_NAME,
          int viewPort_x,
          int viewPort_y,
          int zoomPercentage,
          int proxyPort
  ) {
    String screenModalityType_Size = Config.VIEWPORT;
    int numberOfConcurrentWebdrivers = Config.numOfConcurrentWebdrivers;

    Set<String> keysToCrawl = new HashSet<>();
    Collections.addAll(keysToCrawl, KeysToCrawl.getNavKeysArrowsAndActuationKeys());

    GetTestSubjects gsKRFG = null;

    try {
//      ProcessTimeStamp.initializeProxy(screenModalityType_Size, "start");

      gsKRFG = new GetTestSubjects(SUBJECT_NAME, proxyPort, false);

      Construct_Krawler constttt = new Construct_Krawler(
              gsKRFG,
              screenModalityType_Size,
              viewPort_x,
              viewPort_y,
              zoomPercentage,
              keysToCrawl,
              numberOfConcurrentWebdrivers
      );

      constttt.process();

      System.out.println("end");

    } finally {
      if (gsKRFG != null) {
        try {
          gsKRFG.shutdownWebDriver();
        } catch (Exception e) {
          System.err.println("RunApproachKrawler: failed to shut down GetTestSubjects WebDriver.");
          e.printStackTrace();
        }
      }

      System.out.println("RunApproachKNFG: Finished building KER for size viewport");
    }
  }

  public static void write(String subjectOutputDir, String SUBJECT_NAME) {
    //// get all built reflow KNFG ui states
    Set<UIGraphState> res = Results.getExploredDOMStateSet_size();
    System.out.println("RunApproach:Total number of explored size KER state: " + res.size());

    //// write all built ui states as KER JSON
    WriteUIGraphToJSON w = new WriteUIGraphToJSON(
            subjectOutputDir + "KER" + ".json",
            new ArrayList<UIGraphState>(res),
            "KER"
    );

    w.writeUIGraphOutput();
  }

  public static List<String> prepareData2() {
    //Config.saveSubjectMappingFromGoogleSheetToLocal();
    Config.addSubjectsObjects(); // setup subject objects

    return Config.prepareSubjectsToRun(
            Config.listOfSubjectsToRun,
            Config.listOfSubjectsAlreadyRan
    );
  }

  public static void startMitmproxy(String subjectUniqueName) {
    mitmproxyPath = Config.getMitmProxyBasepath() + File.separator + "mitmdump";
    proxyPort = Config.getNextProxyPort();
    cachePath = Config.cachedSubjectBasepath + File.separator + subjectUniqueName;

    System.out.println("cachePath: " + cachePath);
    System.out.println("proxyPort: " + proxyPort);
    System.out.println("mitmproxyPath: " + mitmproxyPath);

    mitmproxy = new MitmproxyJava(mitmproxyPath, cachePath, proxyPort);
    Config.mitmProxyResourceList.add(mitmproxy);

    try {
      mitmproxy.start();
    } catch (Exception e) {
      System.err.println("RunApproachKrawler: failed to start mitmproxy for subject: " + subjectUniqueName);
      e.printStackTrace();

      throw new RuntimeException("Could not start mitmproxy for subject: " + subjectUniqueName, e);
    }
  }

  public static void stopMitmproxy() {
    if (mitmproxy == null) {
      return;
    }

    try {
      mitmproxy.stop();
    } catch (Exception e) {
      System.err.println("RunApproachKrawler: failed to stop mitmproxy.");
      e.printStackTrace();
    } finally {
      mitmproxy = null;
    }
  }

  public static void hardReset() {
    // reset everything for testing another subject
    Config.shutdownAllDrivers();
    Config.shutdownAllMitmProxies();

//    Config.resetStateToExploreQueue();

    Config.resetUniversalStateIDForCurrentSession();
    Config.resetVisibleDomCtrlElemsXpathsUniversalStateIDMap(); // session state representation map

//    Config.resetDialogCtrlElemsUniversalStateIDMap();

    Config.resetNumOfFinishedStateCrawingThreadsBatches();
  }

  private static void markSubjectAsSuccessfullyProcessed(String subjectUniqueName) {
    String dir =
            Config.getOutputBaseDir()
                    + File.separator
                    + "KER"
                    + File.separator
                    + "successfullyProcessedApproach.csv";

    Set<String> finishedProcessedSubjects =
            new HashSet<>(Utils.readInCsvAsStringArraySingleColumn(dir));

    finishedProcessedSubjects.add(subjectUniqueName);

    Utils.writeOutStringArrayAsCsvSingleColumn(
            dir,
            new ArrayList<>(finishedProcessedSubjects)
    );
  }
}