package edu.uic.cs.dbis.jtv.scorer.third_part;

/**
 * This code is LGPLv3. (http://www.gnu.org/licenses/lgpl-3.0.txt)
 * rcrezende @ gmail . com (Feel free to contact me, if you find bugs, please, report me)
 *  
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmallestBestSnippet {
	public static void main(String[] args) {
		String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras id erat massa. Ullamcorper Lorem Sed ipsum massa risus massa sed id Lorem, ullamcorper nec sollicitudin id, congue sed tortor. Phasellus sed enim leo. Nullam vehicula varius faucibus. Vestibulum augue mi, adipiscing ac sagittis ut amet.";
		String query = "lorem sed massa";

		System.out.printf("\n** Smallest window without cuts size is =%d\n",
				smallestWindowSize(query, text));
	}

	public static int smallestWindowSize(String query, String text) {

		List<TokenInfo> tokensText = tokenize(text);
		List<TokenInfo> tokensQuery = tokenize(query);

		// System.out.println("Text:");
		// printTokens(text, tokensText);
		// System.out.println("\nQuery:");
		// printTokens(query, tokensQuery);

		TokenInfo[][] results = matchPositions(text, query, tokensText,
				tokensQuery);

		int[][] positionList = buildPositionListInput(results);

		int[] S = solve(positionList);
		TokenInfo[] P = new TokenInfo[S.length];
		for (int i = 0; i < positionList.length; ++i)
			P[i] = new TokenInfo(positionList[i][S[i]], tokensQuery.get(i).len);

		Arrays.sort(P);

		int start = P[0].offset;
		int end = P[P.length - 1].offset + P[P.length - 1].len;

		return end - start + 1;

		// System.out
		// .println("\n** Finding continuous best window (cutting some words):");
		// int L = 17; // MAX SNIPPET SIZE
		// int[] W = edgeCut(P, L);
		//
		// int newStart = P[W[0]].offset;
		// int newEnd = P[W[1]].offset + P[W[1]].len;
		//
		// System.out.printf("\n** Best solution: Len: %d\n", newEnd - newStart
		// + 1);
		// System.out.println(emphasize(text, P, W, L));
	}

	/**
	 * <pre>
	 * This algorithm finds the smallest snippet window given positional list of matched tokens.
	 * 
	 * You can use it to minimize the text snippet to show to the user given the positions where
	 * each token has been matched. If there are ties, prefers the most left window.
	 * 
	 * Example: you have a big abstract and want to show, if possible, all matched tokens in a small
	 * text of a search result
	 *  
	 * Each positional list of tokens must be sorted. Each list represents the positions of one token
	 * matched in a text. The last position of each list must be Infinity.
	 * 
	 * @param lists : M[i][j] is the jth token position of the i-th list. The last position must be Infinity!
	 * @return S[i] : S[i] is the token position of the i-th list.
	 * 
	 * Complexity O(dim(M)). (optimum!)
	 * 
	 * Example: 
	 * 
	 * user query="hello world"
	 * field = "the hello is a good way to world be hello. Hi is better to the world";
	 * 
	 * Positional list for that example:
	 *  
	 * M = {{1,9,Infinity},{7,15, Infinity}}
	 * "hello" appears at positions 1 and 9. "world" at 7 and 15.
	 * </pre>
	 */
	static int[] solve(int[][] lists) {
		// start m pointers at the top of each list. (m is the number of lists)
		// move the pointer with min value down and check if the next tuple is
		// better than the current!
		// If yes, select that tuple as the current solution and continue moving
		// the min pointer down until
		// all pointers are at bottom of each list.

		// Weak argument: That works because we have to increase the minimum and
		// decrease the maximum
		// token position to decrease the window size. Since each list is
		// sorted, It is not
		// possible to decrease the maximum. So the only choice is increasing
		// the minimum.
		// Then, the only possible move is select the current min pointer and
		// move it down. CQD!

		int m = lists.length;
		// the current selected element from each list
		int[] pos = new int[m];
		// the current best solution positions
		int[] sol = new int[m];
		// the score (window length) of current solution
		int currSol = Integer.MAX_VALUE;
		while (true) {
			// select the list that has the increasing minimum element
			int minList = argmin(pos, lists);
			// if you can't increase the minimum, stop
			if (minList == -1)
				break;
			// calculate the window size
			int minValue = lists[minList][pos[minList]];
			int maxValue = max(pos, lists);
			int nextSol = maxValue - minValue;
			// update the solution if necessary
			if (nextSol < currSol) {
				currSol = nextSol;
				System.arraycopy(pos, 0, sol, 0, m);
			}
			// update the current minumum element
			pos[minList]++;
		}
		return sol;
	}

	public static double objectiveFunction(TokenInfo[] pos, int start, int end) {
		return end - start + 1;
	}

	public static int[] edgeCut(TokenInfo[] pos, int max) {
		int n = pos.length;
		int end = 0;
		double currSol = 0;
		int solStart = 0;
		int solEnd = 0;
		for (int start = 0; start < n; ++start) {
			for (int nextend = end; nextend < n; nextend++) {
				int total = pos[nextend].offset - pos[start].offset
						+ pos[nextend].len;
				if (total > max)
					break;
				end = nextend;
			}
			double nextSol = objectiveFunction(pos, start, end);
			if (nextSol > currSol) {
				solStart = start;
				solEnd = end;
				currSol = nextSol;
			}
		}
		return new int[] { solStart, solEnd };
	}

	private static int argmin(int[] pos, int[][] v) {
		int min = Integer.MAX_VALUE;
		int arg = -1;
		for (int i = 0; i < v.length; ++i) {
			if (v[i][pos[i]] < min) {
				min = v[i][pos[i]];
				arg = i;
			}
		}
		return arg;
	}

	private static int argmax(int[] pos, int[][] v) {
		int max = -1;
		int arg = -1;
		for (int i = 0; i < v.length; ++i) {
			if (v[i][pos[i]] > max) {
				max = v[i][pos[i]];
				arg = i;
			}
		}
		return arg;
	}

	private static int max(int[] pos, int[][] v) {
		int arg = argmax(pos, v);
		return v[arg][pos[arg]];
	}

	public static class TokenInfo implements Comparable<TokenInfo> {
		public final int offset;
		public final int len;

		TokenInfo(int o, int l) {
			this.offset = o;
			this.len = l;
		}

		public int compareTo(TokenInfo o) {
			return this.offset - o.offset;
		}

		public String toString() {
			return String.format("[%d %d]", offset, len);
		}
	}

	// private static void printTokens(String query, List<TokenInfo>
	// tokensQuery) {
	// for (TokenInfo ti : tokensQuery) {
	// System.out.printf("[[%s] (%d,%d)]",
	// query.substring(ti.offset, ti.len + ti.offset), ti.offset,
	// ti.len);
	// }
	// }

	private static TokenInfo[][] matchPositions(String text, String query,
			List<TokenInfo> tokensText, List<TokenInfo> tokensQuery) {
		// System.out.println("\n==Positions in Text:==\n");
		Map<String, List<TokenInfo>> invertedList = index(text, tokensText);
		TokenInfo[][] results = find(invertedList, query, tokensQuery);
		// for (int i = 0; i < results.length; ++i) {
		// TokenInfo[] positions = results[i];
		// TokenInfo qti = tokensQuery.get(i);
		// String q = query.substring(qti.offset, qti.len + qti.offset);
		// System.out.printf("token (%s) found at positions: {", q);
		// for (TokenInfo ti : positions)
		// System.out.printf(" %d", ti.offset);
		// System.out.println(" }");
		// }
		return results;
	}

	private static int[][] buildPositionListInput(TokenInfo[][] results) {
		int[][] positionList = new int[results.length][];
		for (int j = 0; j < results.length; ++j) {
			TokenInfo[] positions = results[j];
			positionList[j] = new int[positions.length + 1];
			for (int k = 0; k < positions.length; ++k)
				positionList[j][k] = positions[k].offset;
			positionList[j][positions.length] = Integer.MAX_VALUE;
		}
		return positionList;
	}

	// // text emphasize algorithm
	// private static String emphasize(String text, TokenInfo[] P, int[] W, int
	// L) {
	// StringBuffer sb = new StringBuffer(2 * L);
	// int start = P[W[0]].offset;
	// int end = P[W[1]].offset + P[W[1]].len;
	//
	// int j = 0;
	// int close = -1;
	// for (int i = start; i <= end; ++i) {
	// while (j < P.length && P[j].offset < i)
	// j++;
	// char c = text.charAt(i);
	// if (j < P.length && P[j].offset == i) {
	// sb.append("<em>");
	// close = P[j].offset + P[j].len;
	// }
	// if (close == i) {
	// sb.append("</em>");
	// }
	// sb.append(c);
	// }
	// return sb.toString();
	// }

	// inverted indice search
	private static TokenInfo[][] find(Map<String, List<TokenInfo>> invList,
			String query, List<TokenInfo> tokensQuery) {
		TokenInfo[][] res = new TokenInfo[tokensQuery.size()][];
		int i = 0;
		for (TokenInfo ti : tokensQuery) {
			String s = query.substring(ti.offset, ti.len + ti.offset)
					.toLowerCase();
			List<TokenInfo> found = invList.get(s);
			res[i++] = (found == null) ? new TokenInfo[0] : found
					.toArray(new TokenInfo[found.size()]);
		}
		return res;
	}

	// inverted indice builder
	private static Map<String, List<TokenInfo>> index(String text,
			List<TokenInfo> tokens) {
		Map<String, List<TokenInfo>> invList = new HashMap<String, List<TokenInfo>>();
		for (TokenInfo ti : tokens) {
			String s = text.substring(ti.offset, ti.len + ti.offset)
					.toLowerCase();
			List<TokenInfo> list = invList.get(s);
			if (list == null) {
				list = new ArrayList<TokenInfo>();
				invList.put(s, list);
			}
			list.add(ti);
		}
		return invList;
	}

	// simple tokenization
	public static List<TokenInfo> tokenize(String text) {
		List<TokenInfo> tokens = new ArrayList<TokenInfo>();
		int n = text.length();
		boolean state = false;
		int off = 0;
		int len = 0;
		for (int i = 0; i < n; ++i) {
			boolean useChar = Character.isLetterOrDigit(text.charAt(i));
			if (!state && useChar) {
				state = true;
				off = i;
				len = 1;
			} else if (!useChar) {
				state = false;
				tokens.add(new TokenInfo(off, len));
				len = 0;
			} else
				len++;

		}
		if (len > 0)
			tokens.add(new TokenInfo(off, len));
		return tokens;
	}
}