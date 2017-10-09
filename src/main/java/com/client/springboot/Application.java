package com.client.springboot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;

@SpringBootApplication
public class Application implements CommandLineRunner{

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception{

		int digitsToCalculate = 5;
		if (args.length > 0) {
			try{
				digitsToCalculate = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		RestTemplate restTemplate = new RestTemplate();

		// variables initialize block
		BigDecimal s1 = new BigDecimal(0);
		BigDecimal s2 = new BigDecimal(0);
		BigDecimal sub = s1.subtract(s2);
		BigDecimal accuracy = new BigDecimal(Math.pow(10, -digitsToCalculate), MathContext.DECIMAL64);
		long offset = 0;
		long step = 100000;

		// 100 000 operations loop, break when accuracy achieved
		while (true) {
			ExecutorService executorService = Executors.newFixedThreadPool(3);
			List<Callable<BigDecimal>> callableList = generateCallableList(restTemplate, offset, step);

			try {
				List<Future<BigDecimal>> futures = executorService.invokeAll(callableList);

				s1 = s1.add(sumParts(futures));

				BigDecimal nthValue = new BigDecimal(4).divide(new BigDecimal(2 * (offset + step) - 1), MathContext.DECIMAL64);
				s2 = s1.subtract(nthValue);

				offset = offset + step;
				sub = s1.subtract(s2);
				System.out.println("sub=" + sub);
				if (sub.compareTo(accuracy) < 0){
					break;
				}

			} finally {
				executorService.shutdown();
			}
		}

		printResult(s1, s2, digitsToCalculate);
	}

	/**
	 * Generate list of Callable interfaces which will call rest api ]
	 *
	 * @param restTemplate
	 * @param offset
	 * @param step
	 * @return List<Callable<BigDecimal>>
	 */
	private List<Callable<BigDecimal>> generateCallableList(final RestTemplate restTemplate, long offset, long step) {

		List<Callable<BigDecimal>> callableList = new ArrayList<>(3);

		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.TEXT_PLAIN_VALUE);

		for (int i=0; i<3; i++) {
			Map<String, Object> params = new HashMap<>();

			long startIndex = offset +  i*(step/3);

			long endIndex = startIndex + step/3;
			if (i == 2) {
				endIndex++;
			}

			params.put("startIndex", startIndex);
			params.put("endIndex", endIndex);

			callableList.add(new Callable<BigDecimal>() {
				@Override
				public BigDecimal call() throws Exception {
					HttpEntity<String> response = restTemplate.exchange("http://localhost:8080/compute_pi?startIndex={startIndex}&endIndex={endIndex}",
							HttpMethod.GET,
							new HttpEntity<>(headers),
							String.class,
							params.get("startIndex"),
							params.get("endIndex")
					);

					return new BigDecimal(response.getBody(), MathContext.DECIMAL64);
				}
			});
		}

		return callableList;
	}

	/**
	 * Summarize results returned by futures
	 *
	 * @param futures
	 * @return
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private static BigDecimal sumParts(List<Future<BigDecimal>> futures) throws ExecutionException, InterruptedException {

		BigDecimal res = new BigDecimal(0);

		for (Future<BigDecimal> future : futures) {
			res =  res.add(future.get(), MathContext.DECIMAL64);
		}

		return res;
	}

	/**
	 * Print result
	 *
	 * @param s1
	 * @param s2
	 * @param digitsToCalculate
	 */
	private static void printResult(BigDecimal s1, BigDecimal s2, int digitsToCalculate){
		System.out.println("//////////////////////////////////////////////////////////////////////////");
		System.out.println("--------------------------------------------------------------------------");
		System.out.println("-----------------------------RESULT IS HERE: " + s1.add(s2, MathContext.DECIMAL64).divide(new BigDecimal(2), MathContext.DECIMAL64).setScale(digitsToCalculate, BigDecimal.ROUND_DOWN) + "--------------------");
		System.out.println("--------------------------------------------------------------------------");
		System.out.println("//////////////////////////////////////////////////////////////////////////");
	}
}
