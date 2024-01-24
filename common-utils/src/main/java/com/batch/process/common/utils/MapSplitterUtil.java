package com.batch.process.common.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@UtilityClass
@Slf4j
public class MapSplitterUtil {

    /**
     * Amdahl's law for optimum performance suggests optimum threads is 2 times
     * number of cores
     */
    public static final int OPTIMUM_THREAD_COUNT_PER_CORE = 2;

    /**
     * @param <K> An object of type K referred as key
     * @param <V> An Object of type V referred by the key in the map.
     * @param map The map with K as key and V as
     *            Value
     * @return A list of Map objects split by the number of processors
     *         fairly
     */
    public <K, V> List<SortedMap<K, V>> splitMapByProcessor(Map<K, V> map) {
        int processors = Runtime.getRuntime()
                .availableProcessors() * 1;
        return assignWorkToThreads(map, processors);
    }

    /**
     * @param <K>        An object of type K referred as key
     * @param <V>        An Object of type V referred by the key in the map.
     * @param map        The map with K as key and V as
     *                   Value
     * @param splitCount split count by which map will get split
     * @return A list of Map objects split by the splitCount
     *         fairly
     */
    public <K, V> List<SortedMap<K, V>> splitMapBySplitCount(Map<K, V> map, int splitCount) {
        splitCount = splitCount < 1 ? 1 : splitCount;
        splitCount = splitCount > map.size() ? map.size() : splitCount;
        return assignWorkToThreads(map, splitCount);
    }

    /**
     * @param <K> An object of type K referred as key
     * @param <V> An Object of type V referred by the key in the map.
     * @param map The map with K as key and V as
     *            Value
     * @return A list of Map objects split by twice the number of processors
     */
    public <K, V> List<SortedMap<K, V>> optimiseAndAssignThreads(Map<K, V> map) {
        int processors = Runtime.getRuntime()
                .availableProcessors() * OPTIMUM_THREAD_COUNT_PER_CORE;
        return assignWorkToThreads(map, processors);
    }

    public <K, V> List<SortedMap<K, V>> assignWorkToThreads(Map<K, V> map, int splitCount) {
        log.info("Splitting map to {} submaps", splitCount);
        List<SortedMap<K, V>> splittedMaps = new ArrayList<>(splitCount);

        for (int i = 0; i < splitCount; i++) {
            splittedMaps.add(new TreeMap<>());
        }

        Map<Integer, Integer> distributionMap = initializeProcessorDistributionMap(splitCount);

        computeDistribution(map, splitCount, distributionMap);

        allocateAsPerDistribution(map, splittedMaps, distributionMap);
        return splittedMaps.stream()
                .filter(t -> t.size() > 0)
                .collect(Collectors.toList());
    }

    private <V, K> void allocateAsPerDistribution(Map<K, V> map, List<SortedMap<K, V>> splittedMaps,
            Map<Integer, Integer> distributionMap) {
        int counter = 0;
        Iterator<Map.Entry<K, V>> iterator = map.entrySet()
                .iterator();
        Set<Map.Entry<Integer, Integer>> entrySet = distributionMap.entrySet();

        for (Map.Entry<Integer, Integer> entry : entrySet) {
            Integer bucketLimit = entry.getValue();

            while (bucketLimit > 0) {
                Map.Entry<K, V> next = iterator.next();
                splittedMaps.get(counter)
                        .put(next.getKey(), next.getValue());
                bucketLimit--;
            }
            counter++;
        }
    }

    private <K, V> void computeDistribution(Map<K, V> map, int procCount, Map<Integer, Integer> distributionMap) {

        for (int i = 0; i < map.size(); i++) {
            int index = i % procCount;
            Integer distValue = distributionMap.get(index);
            distributionMap.put(index, ++distValue);
        }
    }

    private Map<Integer, Integer> initializeProcessorDistributionMap(int procCount) {
        Map<Integer, Integer> distributionMap = new HashMap<>();

        for (int i = 0; i < procCount; i++) {
            distributionMap.put(i, 0);
        }
        return distributionMap;
    }

    public static void main(String[] args) {
        Map<Integer, List<Integer>> map = new HashMap<>();
        populateMap(map);
        List<SortedMap<Integer, List<Integer>>> splitMap = MapSplitterUtil.splitMapBySplitCount(map,
                Runtime.getRuntime()
                        .availableProcessors() - 1);
        int index = 1;

        for (Map<Integer, List<Integer>> subMap : splitMap) {
            log.info("" + index++);

            Set<Entry<Integer, List<Integer>>> entrySet = subMap.entrySet();

            for (Entry<Integer, List<Integer>> entry : entrySet) {
                log.info(entry.getKey() + "-> " + entry.getValue());
            }
            log.info("--------------");
        }
    }

    private void populateMap(Map<Integer, List<Integer>> map) {

        for (int i = 1; i <= 63; i++) {
            map.put(i, Arrays.asList(i));
        }
    }

}
