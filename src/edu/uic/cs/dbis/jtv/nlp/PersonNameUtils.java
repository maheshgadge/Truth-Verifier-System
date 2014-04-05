package edu.uic.cs.dbis.jtv.nlp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import edu.uic.cs.dbis.jtv.misc.Assert;
import edu.uic.cs.dbis.jtv.misc.TVerifierException;

public class PersonNameUtils {

	private static final Map<String, Double> FREQUENCIES_BY_MALE_FIRST_NAME = loadNamesAndFrequencies(ClassLoader
			.getSystemResource("personNames/dist.male.first").getPath());
	private static final Map<String, Double> FREQUENCIES_BY_FEMALE_FIRST_NAME = loadNamesAndFrequencies(ClassLoader
			.getSystemResource("personNames/dist.female.first").getPath());
	private static final Map<String, Double> FREQUENCIES_BY_LAST_NAME = loadNamesAndFrequencies(ClassLoader
			.getSystemResource("personNames/dist.all.last").getPath());
	private static final Set<String> ALL_NAME_TERMS = new HashSet<String>();
	static {
		ALL_NAME_TERMS.addAll(FREQUENCIES_BY_MALE_FIRST_NAME.keySet());
		ALL_NAME_TERMS.addAll(FREQUENCIES_BY_FEMALE_FIRST_NAME.keySet());
		ALL_NAME_TERMS.addAll(FREQUENCIES_BY_LAST_NAME.keySet());
	}

	private static Map<String, Double> loadNamesAndFrequencies(String fileName) {
		try {
			@SuppressWarnings("unchecked")
			List<String> lines = FileUtils.readLines(new File(fileName));
			HashMap<String, Double> result = new HashMap<String, Double>(
					lines.size());
			for (String line : lines) {
				String[] parts = line.split(" +");
				Assert.isTrue(parts.length == 4,
						"Actual is " + Arrays.toString(parts));

				result.put(parts[0].toLowerCase(Locale.US),
						Double.valueOf(parts[1]));
			}

			return result;
		} catch (IOException e) {
			throw new TVerifierException(e);
		}
	}

	public static boolean isPossiblePersonName(String phrase) {
		List<String> nameParts = TermUtils.tokenize(phrase);
		int length = nameParts.size();

		if (length < 2 || length >= 5) { // too short or too long
			return false;
		}

		if (!ALL_NAME_TERMS.containsAll(nameParts)) {
			return true; // foreign name
		}

		return isWesternName(nameParts);
	}

	private static boolean isWesternName(List<String> nameParts) {
		int length = nameParts.size();

		if (length > 3) {
			// we assume an normal western name contains 3 parts at most
			return false;
		}

		if (length == 3) {
			return isName(nameParts.get(0), nameParts.get(1), nameParts.get(2));
		} else if (length == 2) {
			return isName(nameParts.get(0), nameParts.get(1));
		}

		return false;
	}

	private static boolean isName(String firstName, String lastName) {
		return isFirstName(firstName) && isLastName(lastName);
	}

	private static boolean isLastName(String lastName) {
		lastName = lastName.toLowerCase(Locale.US);
		return FREQUENCIES_BY_LAST_NAME.containsKey(lastName);
	}

	private static boolean isFirstName(String firstName) {
		firstName = firstName.toLowerCase(Locale.US);
		return FREQUENCIES_BY_MALE_FIRST_NAME.containsKey(firstName)
				|| FREQUENCIES_BY_FEMALE_FIRST_NAME.containsKey(firstName);
	}

	private static boolean isName(String firstName, String middleName,
			String lastName) {
		return isName(firstName, lastName)
				&& (isFirstName(middleName) || isLastName(middleName));
	}

	public static void main(String[] args) {
		System.out.println(isPossiblePersonName("james naismith"));
	}

}
