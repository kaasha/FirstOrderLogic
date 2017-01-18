
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class homework {

	static int addSentenceHere = 0;

	public static void main(String args[]) throws IOException {
		Scanner sc;
		KnowledgeBase knowledgeBank[];
		String[] questions;
		try {
			File file = new File("input.txt");
			sc = new Scanner(file);
			int questionCount = sc.nextInt();
			sc.nextLine();
			questions = new String[questionCount];
			knowledgeBank = new KnowledgeBase[questionCount];
			for (int i = 0; i < questionCount; i++) {
				questions[i] = sc.nextLine();
				knowledgeBank[i] = new KnowledgeBase();
			}
			int kb_size = sc.nextInt();
			sc.nextLine();
			for (int i = 0; i < kb_size; i++) {
				String str = sc.nextLine();
				addSentenceHere = i;
				for (int j = 0; j < knowledgeBank.length; j++)
					knowledgeBank[j].Store(str);
			}

			PrintWriter writer = new PrintWriter("output.txt", "UTF-8");
			for (int i = 0; i < questions.length; i++) {
				boolean answer = knowledgeBank[i].askQuestion(questions[i]);
				if (answer)
					writer.println("TRUE");
				else
					writer.println("FALSE");
				/* Delete old Query, and reset the KB */
			}
			writer.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}


class KnowledgeBase {
	// creating queue for predicates
	Queue<Node> node_queue = new LinkedList<Node>();
	HashMap<String, String> mMap = new HashMap<String, String>();
	static HashMap<String, ArrayList<Single>> mSingle = new HashMap<String, ArrayList<Single>>();
	// building kb sentences
	static StringBuilder mKBSentence = new StringBuilder();
	// building query sentences
	Stack<Question> stackOfQuestions = new Stack<>();

	// creating expression tree
	public class Node {
		String mOperation;
		Node left;
		Node right;

		// initializing the nodes
		public Node(String O, Node L, Node R) {
			this.mOperation = O;
			this.left = L;
			this.right = R;
		}
	}

	// creating the expression
	public String createExpression(String string) {
		char[] arr = string.toCharArray();
		int i = 0;
		int counter = 1;
		// adding parenthesis
		while (i < arr.length) {
			StringBuilder strb1 = new StringBuilder();
			if (arr[i] >= 'A' && arr[i] <= 'Z') {
				// appending not operator
				if (i > 0 && arr[i - 1] == '~')
					strb1.append('~');
				while (arr[i] != ')') {
					strb1.append(arr[i]);
					i++;
				}
				strb1.append(')');
				// adding to hashmap
				mMap.put(Integer.toString(counter), strb1.toString());
				// replacing string with counter
				string = string.replace(strb1.toString(), Integer.toString(counter));
				strb1.delete(0, strb1.length());
				counter++;
			}
			i++;
		}
		string = string.replace("=>", ">");
		string = string.replace("^", "&");
		// returns string after substitutions
		return string;
	}

	public Node evaluatExpression(String e) {

		
		Stack<Node> mValues = new Stack<Node>();

		char[] mTokens = e.toCharArray();
		// Creating stack for evaluation of expressions
		Stack<Character> mOperatns = new Stack<Character>();

		for (int i = 0; i < mTokens.length; i++) {

			// skipping space
			if (mTokens[i] == ' ')
				continue;

			if (mTokens[i] == '~')
				mOperatns.push(mTokens[i]);
			// checking for numbers
			if (mTokens[i] >= '0' && mTokens[i] <= '9') {
				StringBuffer sbuffer1 = new StringBuffer();

				while (i < mTokens.length && mTokens[i] >= '0' && mTokens[i] <= '9') {
					sbuffer1.append(mTokens[i]);
					i++;
				}
				i--;
				mValues.push(new Node(sbuffer1.toString(), null, null));
			} else if (mTokens[i] == '(')
				mOperatns.push(mTokens[i]);

			else if (mTokens[i] == ')') {
				while (mOperatns.peek() != '(')
					mValues.push(applyOp(mOperatns.pop(), mValues.pop(), mValues.pop()));
				mOperatns.pop();
				// evaluating the expression
				if (!mOperatns.isEmpty())
					if (mOperatns.peek() == '~') {
						mValues.push(applyOp(mOperatns.pop(), mValues.pop(), null));
					}
			} else if (mTokens[i] == '|' || mTokens[i] == '&' || mTokens[i] == '>') {
				while (!mOperatns.empty() && hasPrecedence(mTokens[i], mOperatns.peek()))
					mValues.push(applyOp(mOperatns.pop(), mValues.pop(), mValues.pop()));
				mOperatns.push(mTokens[i]);
			}
		}
		// checking whether stack is empty
		while (!mOperatns.empty())
			mValues.push(applyOp(mOperatns.pop(), mValues.pop(), mValues.pop()));

		return mValues.pop();
	}

	// Creates expression tree
	void traverse(Node root) {

		if (root != null) {
			// adding parenthesis to sentence
			mKBSentence.append("(");
			traverse(root.left);
			if (root.left == null && root.right == null) {
				if (root.mOperation.contains("~~"))
					root.mOperation = root.mOperation.substring(2);

				if (root.mOperation.contains("~"))
					mKBSentence.append(" ~" + mMap.get(root.mOperation.substring(1)) + " ");
				else
					mKBSentence.append(" " + mMap.get(root.mOperation) + " ");

			} else
				mKBSentence.append(" " + root.mOperation + " ");
			// doing inorder traversal
			traverse(root.right);
			mKBSentence.append(")");
		}
	}

	public Node MoveNegs(Node root) {
		// Move negative inwards
		if (root != null) {
			root.left = MoveNegs(root.left);
			if (root.mOperation.equals("~")) {
				root = killnegs(root.right);
				root = MoveNegs(root);
				return root;
			}
			root.right = MoveNegs(root.right);
		}
		return root;
	}

	public boolean hasPrecedence(char op1, char op2) {
		// giving preferance to parenthesis
		if (op2 == '(' || op2 == ')')
			return false;
		else
			return true;
	}

	public Node applyOp(char op, Node b, Node a) {
		// pushing nodes with operators
		return (new Node(op + "", a, b));
	}

	public Node distributeAnd(Node root) {
		// Distribute and over the expression
		if (root != null) {
			root.left = distributeAnd(root.left);
			if (root.mOperation.equals("|")) {
				// considering all 3 cases
				if (root.left.mOperation.equals("&") && root.right.mOperation.equals("&")) {
					root = leftAndRightAnd(root);
					root.mOperation = "&";
				} else if (root.right.mOperation.equals("&")) {
					root = rightAndLefttOr(root);
					root.mOperation = "&";
					root.right.mOperation = "|";
				} else if (root.left.mOperation.equals("&")) {
					root = leftAndRightOr(root);
					root.mOperation = "&";
					root.left.mOperation = "|";
				}
				root.right = distributeAnd(root.right);
			}
		}

		return root;
	}

	public Node RemoveImplies(Node root) {
		// Implies elimination
		if (root != null) {
			root.left = RemoveImplies(root.left);
			if (root.mOperation.equals(">")) {
				root.mOperation = "|";
				root.left = killnegs(root.left);
			}
			root.right = RemoveImplies(root.right);
		}
		return root;
	}

	public Node killnegs(Node root) {
		// Remove extra negatives
		if (root != null) {
			boolean flag = false;
			root.left = killnegs(root.left);
			// not followed by and turns or
			if (root.mOperation.equals("&"))
				root.mOperation = "|";
			// not followed by or turns and
			else if (root.mOperation.equals("|"))
				root.mOperation = "&";
			else if (root.mOperation.equals("~"))
				flag = true;
			else
				root.mOperation = "~" + root.mOperation;
			if (flag)
				root = RemoveImplies(root.right);
			else
				root.right = killnegs(root.right);
		}
		return root;
	}

	Node leftAndRightOr(Node root) {
		// Or in left and right
		Node copy = root.right;
		Node newRight = new Node("|", root.left.right, copy);
		root.right = newRight;
		root.left.right = copy;
		return root;
	}

	Node rightAndLefttOr(Node root) {
		// Or in right and left
		Node copy = root.left;
		Node newLeft = new Node("|", copy, root.right.left);
		root.left = newLeft;
		root.right.left = copy;
		return root;
	}

	public void getSingle(String cnf) {
		// Get conjuncts as single clauses for kb
		String conjunctList[] = cnf.split("&");
		// Create list of conjuncts
		ArrayList<String> list = new ArrayList<String>();
		for (String x : conjunctList) {
			char[] Array = x.toCharArray();
			int i = 0;
			while (i < Array.length) {
				StringBuilder str1 = new StringBuilder();
				// Check for predicate
				if (Array[i] >= 'A' && Array[i] <= 'Z') {
					if (i > 0 && Array[i - 1] == '~')
						str1.append('~');
					while (Array[i] != ')') {
						str1.append(Array[i]);
						i++;
					}
					str1.append(')');
					if (!list.contains(str1.toString()))
						list.add(str1.toString());
					str1.delete(0, str1.length());
				} else
					i++;
			}

			// Add list to KB
			kbc(list);
			list.clear();
		}

	}

	Node leftAndRightAnd(Node root) {

		Node ____ = root.left.left;
		Node __ = root.left.right;
		Node _$_ = root.right.left;
		Node _$$_ = root.right.right;

		Node _$$ = new Node("|", ____, _$_);
		Node _$1 = new Node("|", ____, _$$_);
		Node _1$ = new Node("|", __, _$_);
		Node _11 = new Node("|", __, _$$_);
		// Exchanging the roots
		root.left.left = _$$;
		root.left.right = _$1;
		root.right.left = _1$;
		root.right.right = _11;

		return root;
	}

	public void kbc(ArrayList<String> clauseList) {
		Sentence cls = new Sentence(clauseList);
		for (ArrayList<Single> list : cls.mORs.values()) {

			String key = list.get(0).pred();
			ArrayList<Single> clList;
			if (!mSingle.containsKey(key))
				clList = new ArrayList<Single>();
			else
				clList = mSingle.get(key);
			clList.addAll(list);
			mSingle.put(key, clList);
		}
	}

	public boolean dfs(String q) {

		// do a dfs traversal
		q = q.substring(q.indexOf("(") + 1, q.lastIndexOf(")")).trim();
		q = "~" + q;
		// replace the negatives
		q = q.replaceAll("~~", "");
		Sentence init = new Sentence(q);
		Question root = new Question(init, init.mVariables, new HashSet<String>());
		stackOfQuestions.push(root);
		boolean flag = false;
		while (!stackOfQuestions.isEmpty()) {
			Question myRunningQuery = stackOfQuestions.pop();

			if (myRunningQuery != null) {
				if (myRunningQuery.checkNull()) {
					flag = true;
					break;
				}
				stackOfQuestions.addAll(myRunningQuery.visit());
			}
		}
		// return the actual value of query
		return flag;
	}

	public static ArrayList<Sentence> find(String predicate) {

		ArrayList<Sentence> retVal = new ArrayList<>();
		ArrayList<Single> fluent = mSingle.get(predicate.trim());
		// List of predicates
		if (fluent != null)
			for (Single key : fluent) {
				retVal.add(key.pSentence);
			}
		return retVal;
	}

	// Stote all the strings
	public void Store(String s) {
		Node root = evaluatExpression(createExpression(s));
		traverse(root);
		mKBSentence.delete(0, mKBSentence.length());

		RemoveImplies(root);
		traverse(root);
		mKBSentence.delete(0, mKBSentence.length());

		MoveNegs(root);
		traverse(root);
		mKBSentence.delete(0, mKBSentence.length());

		distributeAnd(root);
		traverse(root);
		getSingle(mKBSentence.toString().replace("~~", ""));

		mKBSentence.delete(0, mKBSentence.length());
	}

	public boolean askQuestion(String question) {
		// Check solution for each sentence
		Node root = evaluatExpression(createExpression(question));
		traverse(root);
		mKBSentence.delete(0, mKBSentence.length());

		RemoveImplies(root);
		traverse(root);
		mKBSentence.delete(0, mKBSentence.length());

		MoveNegs(root);
		traverse(root);
		mKBSentence.delete(0, mKBSentence.length());

		distributeAnd(root);
		traverse(root);
		return dfs(mKBSentence.toString().replace("~~", ""));
	}
}


class Question {

	Sentence runningSentence;
	HashMap<String, String> runningVariables = new HashMap<>();
	HashSet<String> visitedPath = new HashSet<>();

	public Question(Sentence runningQuery, HashMap<String, String> runningVars, HashSet<String> thisisDone) {
		this.runningSentence = runningQuery;
		this.runningVariables = runningVars;
		this.visitedPath = new HashSet<>(thisisDone);
	}

	boolean checkNull() {
		return runningSentence.mORs.size() == 0;
	}

	static int count = 0;

	ArrayList<Question> visit() {
		ArrayList<Sentence> dataInKB = new ArrayList<>();
		ArrayList<Question> children = new ArrayList<>();

		count++;

		HashMap<String, String> temporaryVariables = new HashMap<String, String>(runningVariables);
		for (ArrayList<Single> templist : runningSentence.mORs.values()) {
			for (Single pred : templist) {

				dataInKB = KnowledgeBase.find(pred.not());

				for (Sentence keyClause : dataInKB) {
					runningVariables = new HashMap<>(temporaryVariables);
					runningSentence.mVariables = runningVariables;
					children.add(myUnify(runningSentence, keyClause));
				}
			}
		}
		return children;
	}

	Question myUnify(Sentence ss, Sentence qq) {

		boolean doWeHaveAUnification = false;

		Sentence q = new Sentence(ss);
		Sentence kb = new Sentence(qq);

		String queryKey = q.toString() + "," + kb.toString();

		HashMap<String, String> vars = new HashMap<>(q.mVariables);

		ArrayList<Single> delqur = new ArrayList<>();
		ArrayList<Single> delkb = new ArrayList<>();

		if (!visitedPath.contains(queryKey))
			for (ArrayList<Single> list : q.mORs.values()) {
				for (Single a : list)
					if (kb.mORs.containsKey(a.not())) {
						ArrayList<Single> kblist = kb.mORs.get(a.not());
						for (Single b : kblist) {
							FirstOrderLogicInterface aa = a;
							FirstOrderLogicInterface bb = b;
							if (a.visited == false && b.visited == false) {
								HashMap<String, String> temp = unite(aa, bb, new HashMap<String, String>(vars));
								if (temp != null) {
									doWeHaveAUnification = true;
									a.visited = b.visited = true;
									vars = temp;
									delqur.add(a);
									delkb.add(b);
									break;
								}
							}
						}
					}
			}

		if (doWeHaveAUnification) {

			visitedPath.add(queryKey);

			for (int i = 0; i < delqur.size(); i++) {
				q.deleteSingle(delqur.get(i));
				kb.deleteSingle(delkb.get(i));
			}

			Sentence a = join(q, kb);
			return (new Question(a, new HashMap<>(vars), visitedPath));
		}
		return null;

	}

	public HashMap<String, String> unifyVar(Parameters p, FirstOrderLogicInterface foli,
			HashMap<String, String> map) {

		if (map == null)
			return null;

		for (String v : map.keySet()) {
			if (v.equals(p.mVar) && map.get(v) != null) {
				return unite(new Parameters(map.get(v)), foli, map);
			}
		}

		if (map.keySet().contains(foli.toString()) && (map.get(foli.toString()) != null))
			return unite(p, new Parameters(map.get(foli.toString())), map);

		if (!map.keySet().contains(p.mVar) || (map.get(p.mVar) == null)) {
			if (map.get(p.mVar) != null) {
				if (map.get(p.mVar).equals(map.get(p.mVar).toLowerCase()))
					map.put(p.mVar, foli.toString());
			}

			map.put(p.mVar, foli.toString());
			return map;
		}
		return map;
	}

	public HashMap<String, String> unite(FirstOrderLogicInterface x, FirstOrderLogicInterface y,
			HashMap<String, String> vars) {

		if (vars == null)
			return null;

		if (x.equals(y)) {
			return vars;
		} else if (x instanceof Parameters && !((Parameters) x).isVar && y instanceof Parameters
				&& !((Parameters) y).isVar) {
			return ((Parameters) x).mVar.equals(((Parameters) y).mVar) ? vars : null;
		} else if (x instanceof Parameters && ((Parameters) x).isVar) {
			return unifyVar((Parameters) x, y, vars);
		} else if (y instanceof Parameters && ((Parameters) y).isVar) {
			return unifyVar((Parameters) y, x, vars);
		} else if (x instanceof Single && y instanceof Single) {
			return unify(((Single) x).mParameter, ((Single) y).mParameter, vars);
		} else {
			return null;
		}

	}

	public HashMap<String, String> unify(LinkedHashSet<Parameters> x, LinkedHashSet<Parameters> y,
			HashMap<String, String> vars) {

		if (vars == null)
			return null;

		if (x.size() != y.size()) {
			return null;
		} else if (x.size() == 0 && y.size() == 0)
			return vars;
		else if (x.size() == 1 && y.size() == 1)
			return unite(x.iterator().next(), y.iterator().next(), vars);
		else {

			LinkedHashSet<Parameters> tempy = new LinkedHashSet<>(y);
			LinkedHashSet<Parameters> tempx = new LinkedHashSet<>(x);

			FirstOrderLogicInterface a = tempx.iterator().next();
			FirstOrderLogicInterface b = tempy.iterator().next();
			vars = unite(a, b, vars);

			tempx.remove(a);
			tempy.remove(b);
			return unify(tempx, tempy, vars);
		}

	}

	public Sentence join(Sentence a, Sentence b) {
		HashMap<String, ArrayList<Single>> temp = new HashMap<String, ArrayList<Single>>(a.mORs);
		temp.putAll(b.mORs);
		HashMap<String, String> tempvars = new HashMap<>(a.mVariables);
		return new Sentence(temp, tempvars);
	}
}

class Sentence {

	HashMap<String, ArrayList<Single>> mORs = new HashMap<>();
	HashMap<String, String> mVariables = new HashMap<>();

	Sentence(String exprs) {
		String tempContainer[] = exprs.split(" $ ");
		ArrayList<Single> tempList;

		for (String i : tempContainer) {
			String key = i.substring(0, i.indexOf('('));
			if (!mORs.containsKey(key))
				tempList = new ArrayList<>();
			else
				tempList = mORs.get(key);
			tempList.add(new Single(i, this));
			mORs.put(key, tempList);
		}

		for (ArrayList<Single> list : mORs.values()) {
			for (Single fluent : list) {

				Iterator<Parameters> point = fluent.mParameter.iterator();
				while (point.hasNext())
					mVariables.put(point.next().mVar, null);
			}
		}
	}

	Sentence(ArrayList<String> listOfClauses) {
		ArrayList<Single> tempList;

		for (String i : listOfClauses) {

			String key = i.substring(0, i.indexOf('(')).trim();
			if (!mORs.containsKey(key))
				tempList = new ArrayList<>();
			else
				tempList = mORs.get(key);
			tempList.add(new Single(i, this));
			mORs.put(key, tempList);
		}
		for (ArrayList<Single> list : mORs.values()) {
			for (Single fluents : list) {

				Iterator<Parameters> point = fluents.mParameter.iterator();
				while (point.hasNext())
					mVariables.put(point.next().mVar, null);
			}
		}
	}

	public void deleteSingle(Single x) {
		this.mORs.get(x.pred()).remove(x);
		if (this.mORs.get(x.pred()).size() == 0)
			this.mORs.remove(x.pred());
	}

	Sentence(HashMap<String, ArrayList<Single>> ors, HashMap<String, String> v) {
		this.mORs = ors;
		this.mVariables = v;
	}

	@Override
	public String toString() {
		String ret = "";

		for (ArrayList<Single> list : mORs.values())
			for (Single atom : list) {
				ret += atom.mVal + " | ";

			}
		return ret;
	}

	Sentence(Sentence sentence) {
		mORs = new HashMap<String, ArrayList<Single>>();
		for (ArrayList<Single> list : sentence.mORs.values())
			for (Single fluents : list) {
				ArrayList<Single> tempList;
				if (mORs.containsKey(fluents.pred()))
					tempList = mORs.get(fluents.pred());
				else
					tempList = new ArrayList<>();
				tempList.add(new Single(fluents));
				mORs.put(fluents.pred(), tempList);
			}
		mVariables = new HashMap<>(sentence.mVariables);
	}
}

class Single implements FirstOrderLogicInterface, Comparable {

	String mVal;
	Sentence pSentence;
	boolean visited = false;
	LinkedHashSet<Parameters> mParameter = new LinkedHashSet<>();

	public Single(String value, Sentence parent) {
		this.mVal = value.trim();
		this.pSentence = parent;
		String temp[];
		temp = value.substring(value.lastIndexOf("(") + 1, value.indexOf(")")).split(",");
		for (String i : temp)
			this.mParameter.add(new Parameters(i.trim()));
	}

	public Single(Single a) {
		mVal = a.mVal;
		pSentence = a.pSentence;
		mParameter = new LinkedHashSet<>(a.mParameter);
	}

	String pred() {
		return mVal.substring(0, mVal.indexOf("("));
	}

	String not() {
		String ret = mVal.substring(0, mVal.indexOf("("));
		if (mVal.contains("~")) {
			return ret.replace("~", "");
		}
		return "~" + ret;
	}

	@Override
	public boolean isNested() {
		return true;
	}

	@Override
	public int compareTo(Object o) {
		return mVal.compareTo(((Single) o).mVal);
	}
}

class Parameters implements FirstOrderLogicInterface, Comparable {
	String mVar;
	boolean isVar = false;

	public Parameters(String var) {
		var = var.trim();
		isVar = var.equals(var.toLowerCase());
		if (isVar && !var.matches(".*\\d+.*"))
			var += homework.addSentenceHere;
		this.mVar = var.trim();
	}

	@Override
	public boolean isNested() {
		return false;
	}

	@Override
	public String toString() {
		return this.mVar;
	}

	@Override
	public int compareTo(Object o) {
		if (o instanceof Parameters) {
			return (mVar.equals(((Parameters) o).mVar) ? 0 : -1);
		}
		return -1;
	}

}

interface FirstOrderLogicInterface {
	boolean isNested();
	String toString();
}
