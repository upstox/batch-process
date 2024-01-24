package com.batch.process.manager;

import static com.batch.process.common.BatchConstant.MANAGER_TIME;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.StopWatch;

import com.batch.process.dataservice.CleanUpDataService;
import com.batch.process.dataservice.ContextMap;
import com.batch.process.dataservice.DataService;
import com.batch.process.dataservice.ListDataService;
import com.batch.process.dataservice.SingleDataService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepExecutor {

	private StepExecutor() {

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void execute(JobExecution jobExecution, DataService<?> dataService) {
		JobParameters jobParameters = jobExecution.getJobParameters();
		Long id = jobExecution.getId();
		Map<String, JobParameter> parameters = jobParameters.getParameters();
		Map<String, Object> parameterMap = new HashMap<>();

		ExecutionContext executionContext = jobExecution.getExecutionContext();
		log.info("Invoking step -> {}", dataService.getDataServiceName());
		// Update the contextMap with info to be propagated to next step
		ContextMap contextMap = new ContextMap();
		log.info("Step Context map parameter -");
		executionContext.entrySet().forEach(e -> {
			String key = e.getKey();
			contextMap.put(key, e.getValue());
			log.info("\t\t " + (String.format("%-30s", key)) + " -> {}", contextMap.get(key).toString());
		});
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// Pass the job parameters to each data service
		for (Entry<String, JobParameter> entrySet : parameters.entrySet()) {
			parameterMap.put(entrySet.getKey(), entrySet.getValue());
		}

		if (dataService instanceof ListDataService) {
			ListDataService listDataService = (ListDataService) dataService;

			List data = listDataService.getData(id.toString(), Collections.unmodifiableMap(parameterMap), contextMap);
			listDataService.storeData(id.toString(), data, parameterMap, contextMap);
		} else if (dataService instanceof SingleDataService) {
			SingleDataService singleDataService = (SingleDataService) dataService;
			Object data = singleDataService.getData(id.toString(), Collections.unmodifiableMap(parameterMap),
					contextMap);
			singleDataService.storeData(id.toString(), data, Collections.unmodifiableMap(parameterMap), contextMap);
		} else if (dataService instanceof CleanUpDataService) {
			CleanUpDataService cleanUpDataService = (CleanUpDataService) dataService;
			cleanUpDataService.cleanUp(id.toString(), parameterMap, contextMap);
		}
		// Update the execution context back with info from current step
		stopWatch.stop();

		if (Objects.nonNull(contextMap.get(MANAGER_TIME))) {
			Object managerTimeObject = contextMap.get(MANAGER_TIME);
			long previousStepTime = Long.parseLong(managerTimeObject.toString());
			contextMap.put(MANAGER_TIME, stopWatch.getLastTaskTimeMillis() + previousStepTime);
		}

		contextMap.entrySet().forEach(e -> executionContext.put(e.getKey(), e.getValue()));
	}
}
