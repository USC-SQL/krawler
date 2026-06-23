package edu.usc.sql.krawler.core;


import edu.usc.sql.krawler.buildgraphs.UIState;
import edu.usc.sql.krawler.graphs.UIGraphState;
import lombok.Getter;

import java.util.*;

public class Results {


    public static int universalStateID = 0;
    public static Map<UIState, Integer> nodesXpathsUniversalStateIDMap = new HashMap<>();
    public static int getUniqueUniversalStateID(UIState state) {
        if (nodesXpathsUniversalStateIDMap.containsKey(state)) {
            System.out.println("Config: visible Dom ctrl elems state has been seen " + nodesXpathsUniversalStateIDMap.get(state));
            return nodesXpathsUniversalStateIDMap.get(state);
        } else {        // if visible Dom ctrl elems state has not been seen, then assign new id
            System.out.println("Config: visible Dom ctrl elems state has not been seen");
            int newlyAssignedStateID = universalStateID++;
            nodesXpathsUniversalStateIDMap.put(state, newlyAssignedStateID);        // add to map
            return newlyAssignedStateID;
        }
    }






    @Getter
    public static Set<UIGraphState> exploredDOMStateSet_size = new HashSet<>();
    public static void addToExploredDOMStateSet_size(UIGraphState domState) {
        exploredDOMStateSet_size.add(domState);
    }


    @Getter
    static List<String> res = new ArrayList<>();

    public static void adddd(String s){
        res.add(s);
    }


    public static void show(){
        for (String s : res){
            System.out.println(s);


        }

    }

    @Getter
    public static Set<UIState> visitedStates = new HashSet<>();
    public static void addToVisitedStates(UIState state){
        visitedStates.add(state);
    }



    public static boolean isVisitedState(UIState state){
        if(visitedStates.contains(state)){
            return true;
        } else {
            return false;
        }
    }



}
