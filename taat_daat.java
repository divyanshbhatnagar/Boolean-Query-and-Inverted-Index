package ir_proj2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.text.Document;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;




public class demo
{
	//declaring variables for number of comparisons 
	static int comp = 0;
	static int comp1 = 0;
	static int comp2 = 0;
	static int comp3 = 0;
	public static void main (String[] args) throws IOException
	{
		String path_of_index = args[0];
		String outputName = args[1];
		String inputName = args[2];
		String aLine;
		ArrayList<String> inputString = new ArrayList<String>();
		File input= new File(inputName);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input),"UTF-8"));
		BufferedWriter o = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputName),"UTF-8"));

		while((aLine = br.readLine()) != null)
		{
			inputString.add(aLine);
		}
		FileSystem fs = FileSystems.getDefault();
		Path path1 = fs.getPath(path_of_index);
		HashMap<String,LinkedList<Integer>> mapInversion = new HashMap<String,LinkedList<Integer>>();
		IndexReader reader = DirectoryReader.open(FSDirectory.open(path1));


		Fields fields = MultiFields.getFields(reader);
		//building inverted index
		for (String field : fields){
			//excluding fields "id" and "version"
			if(!field.equals("id") && !field.equals("_version_")){

				Terms terms = fields.terms(field);
				TermsEnum termsEnum = terms.iterator();
				int count = 0;
				BytesRef term;
				while ( (term = termsEnum.next()) != null) {
					String term_a = term.utf8ToString();
					PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term, PostingsEnum.FREQS);
					int a;
					LinkedList<Integer> a1= new LinkedList<Integer>();
					while ((a = postingsEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS)
					{
						int doc = postingsEnum.docID();
						a1.add(doc);
						mapInversion.put(term_a, a1);
					}
					count++;
				}
			}
		} 
		//passing input query to functions
		for(int k=0; k<inputString.size(); k++){
			String t = inputString.get(k);	 
			termAtATime(t, mapInversion, o ); 
			docAtaTimetAnd(t, mapInversion, o);
			docAtaTimetOr(t, mapInversion, o);
			comp=0;
			comp1=0;
			comp2=0;
			comp3=0;
		}
		o.close();
	}
	//function for term at a time query
	public static void termAtATime(String a, HashMap<String,LinkedList<Integer>> index, BufferedWriter o) throws IOException{
		
		LinkedList<Integer> andList = new LinkedList<Integer>();
		LinkedList<Integer> interimAnd = new LinkedList<Integer>();
		LinkedList<Integer> interimOr = new LinkedList<Integer>();
		LinkedList<Integer> orList = new LinkedList<Integer>();
		//splitting the query into terms
		String[] terms = a.split("\\s+");
		/*using an intermediate list to store results of term at a time AND and OR queries
		 * we take 2 terms compute the result and add it to intermediate list the compare the 
		 * next term with this intermediate result. The final value in this list is our answer.
		 */
		interimAnd = index.get(terms[0]);
		interimOr = index.get(terms[0]);
		//call to function for getting postings list of each term
		for(int i=0; i<terms.length; i++)
		{
			getPostingsList(terms[i],index, o);
		}
		//calling term at a time AND function for each term 
		for(int i=1; i<terms.length; i++)
		{
			interimAnd = termAtATimeAnd(interimAnd,index.get(terms[i]),index);
		}
		andList = interimAnd;
		//printing the output to file
		System.out.println("TaatAnd");
		o.write("TaatAnd" + "\r\n");
		System.out.println(a);
		o.write(a + "\r\n");
		System.out.print("Results: ");
		o.write("Results: ");
		// checking for "empty" result
		if(andList.size() != 0){
			for(int i=0; i<andList.size(); i++)
			{

				System.out.print(andList.get(i) + " ");
				o.write(andList.get(i) + " ");

			}
		}
		else
		{
			System.out.print("empty");
			o.write("empty");
		}
		o.write("\r\n");
		System.out.println("\n" + "Number of documents in results : " + andList.size());
		o.write("Number of documents in results: " + andList.size() + "\r\n");
		System.out.println("Number of comparisons: " + comp);
		o.write("Number of comparisons: " + comp);
		//calling term at a time OR function for each term
		for(int i=1; i<terms.length; i++)
		{
			interimOr = termAtATimeOr(interimOr,index.get(terms[i]),index);
		}
		orList = interimOr;
		System.out.println("TaatOr");
		o.write("\r\n" + "TaatOr" + "\r\n");
		System.out.println(a);
		o.write(a + "\r\n");
		System.out.print("Results: ");
		o.write("Results: ");
		for(int i=0; i<orList.size(); i++)
		{
			System.out.print(orList.get(i) + " ");
			o.write(orList.get(i) + " ");
		}
		System.out.println("\n" + "Number of documents in results : " + orList.size());
		o.write("\r\n" + "Number of documents in results: " + orList.size() + "\r\n");
		System.out.println("Number of comparisons: " + comp1);
		o.write("Number of comparisons: " + comp1);
	}
	//Term at a time AND function
	public static LinkedList<Integer> termAtATimeAnd(LinkedList<Integer> x, LinkedList<Integer> y, HashMap<String,LinkedList<Integer>> index)
	{
		//initializing a linked list which will store the result
		LinkedList<Integer> ans = new LinkedList<Integer>();

		int p=0, q=0;
		//compare the postings list of both terms
		while(!x.equals(null) && !y.equals(null) && p<x.size() && q<y.size())
		{
			//if both id's equal add to result list and increment the comparisons
			if(x.get(p).equals(y.get(q)))
			{
				ans.add(x.get(p));
				p++;
				q++;
				comp++;
				
			}
			//increment the pointer for smaller doc id and increment the comparison
			else if(x.get(p)<y.get(q))
			{
				p++;
				comp++;
			}
			else
			{
				q++;
				comp++;
			}
		}
		return ans;
	}
	//Term at a time OR function
	public static LinkedList<Integer> termAtATimeOr(LinkedList<Integer> x, LinkedList<Integer> y, HashMap<String,LinkedList<Integer>> index)
	{
		//initializing a linked list which will store the result
		LinkedList<Integer> ans = new LinkedList<Integer>();

		int p=0, q=0;
		//compare the postings list of both terms
		while(!x.equals(null) && !y.equals(null) && p<x.size() && q<y.size())
		{
			//if both id's equal add to result list and increment the comparisons
			if(x.get(p).equals(y.get(q)))
			{
				ans.add(x.get(p));
				p++;
				q++;
				comp1++;
			}
			//increment the pointer for smaller doc id and increment the comparison and add to result
			else if(x.get(p)<y.get(q))
			{
				ans.add(x.get(p));
				p++;
				comp1++;
			}
			else
			{
				ans.add(y.get(q));
				q++;
				comp1++;
			}
		}
		//Add the remaining terms to the result list
		while(p<x.size()){
			ans.add(x.get(p));
			p++;
		}
		while(q<y.size()){
			ans.add(y.get(q));
			q++;
		}
		return ans;
	}
	//Function to get the postings list of each term
	public static void getPostingsList(String x, HashMap<String,LinkedList<Integer>> index, BufferedWriter o) throws IOException
	{
		
		LinkedList<Integer> l1 = new LinkedList<Integer>();

		l1 = index.get(x);

		System.out.println("GetPostings" + "\n" + x);
		o.write("GetPostings");
		o.write("\r\n");
		o.write(x);
		o.write("\r\n");

		System.out.print("Postings list: ");
		o.write("Postings list: " );
		for(int i=0; i<l1.size(); i++)
		{

			System.out.print(l1.get(i) + " ");
			o.write(l1.get(i) + " ");
		}
		System.out.println("");
		o.write("\r\n");
	}
	//Function for document at a time OR query 
	public static void docAtaTimetOr(String q, HashMap<String,LinkedList<Integer>> index, BufferedWriter o) throws IOException
	{ 
		//Creating an array list of linked list type to store the query terms posting list
		ArrayList<LinkedList<Integer>> queryTerms = new ArrayList<LinkedList<Integer>>();
		LinkedList<Integer> temp = new LinkedList<Integer>();
		LinkedList<Integer> result = new LinkedList<Integer>();
		LinkedList<Integer> ref = new LinkedList<Integer>();

		String[] query = q.split("\\s+");
		int[] sizeOfQuery = new int[query.length];
		//We find the query term list of maximum size to use it as a reference list
		for(int i=0; i<query.length; i++)
		{
			temp = index.get(query[i]);
			queryTerms.add(temp);
			sizeOfQuery[i] = queryTerms.get(i).size();
		}
		int maximumSize = sizeOfQuery[0];
		int maximumPosting = 0;
		for(int j=1; j<query.length; j++)
		{
			if(sizeOfQuery[j]>maximumSize)
			{
				maximumSize = sizeOfQuery[j];
				maximumPosting = j;
			}
		}
		//We add the maximum posting list term at the start of our array list
		ref = queryTerms.get(maximumPosting);
		queryTerms.add(0,ref);
		queryTerms.remove(maximumPosting+1);
		 int size = queryTerms.size();
		 int[] pointersArray = new int [size];
		 int[] sizesArray = new int[size];

		 for(int i = 0; i<queryTerms.size(); i++)
		 {
			 sizesArray[i] = queryTerms.get(i).size();
			 pointersArray[i] = 0;
		 }

		 boolean end = false;

		 while (end == false)
		 {
			 int c = 0;
			 for (int z = 0; z<size; z++)
			 {
				 if (pointersArray[z] == sizesArray[z]){
					 c++;
				 }
			 }
			 if(c==size){
				 end = true;

			 }

			 if(end==false){
				 int j = 0;
				 while(j<size){
					 if(pointersArray[j]<sizesArray[j]){
						 long element = queryTerms.get(j).get(pointersArray[j]);
						 boolean flag = true;
						 if(queryTerms.size()>1){
						 comp3++;
						 }
						 for(int i=0; i<result.size(); i++){
							 if(element == result.get(i)){
								 flag = false;
								 pointersArray[j]++;
							 }
						 }
						 if(flag==true){
							 result.add(queryTerms.get(j).get(pointersArray[j]));
							 pointersArray[j]++;
						 }
					 }
					 j++;
				 }
			 }
		 }
		 Collections.sort(result);
		 System.out.println("DaatOr" + "\n" + q);
		 o.write("DaatOr");
		 o.write("\r\n" + q);
		 System.out.print("Results: ");
		 o.write("\r\n" + "Results: ");
		 for (int i=0; i<result.size(); i++ )
		 {
			 System.out.print(result.get(i) + " ");
			 o.write(result.get(i) + " ");
		 }
		 System.out.println("\nNumber of documents in results: " + result.size());
		 o.write("\r\n" + "Number of documents in results: " + result.size() + "\r\n");
		 System.out.println("Number of comparisons: " + comp3);
		 o.write("Number of comparisons: " + comp3 + "\r\n");
	}
	//Function for document at a time AND query
	public static void docAtaTimetAnd(String query, HashMap<String,LinkedList<Integer>> index, BufferedWriter o ) throws IOException
	{
		//Creating an array list of linked list type to store query
		ArrayList<LinkedList<Integer>> queryTerms = new ArrayList<LinkedList<Integer>>();
		LinkedList<Integer> temp = new LinkedList<Integer>();
		LinkedList<Integer> result = new LinkedList<Integer>();
		LinkedList<Integer> ref = new LinkedList<Integer>();
		String[] q = query.split("\\s+");
		int[] sizeOfQuery = new int[q.length];
		////We find the query term list of minimum size to use it as a reference list
		for(int i=0; i<q.length; i++)
		{
			temp = index.get(q[i]);
			queryTerms.add(temp);
			sizeOfQuery[i] = queryTerms.get(i).size();
		}
		int minimumSize = sizeOfQuery[0];
		int minimumPosting = 0;
		for(int j=1; j<sizeOfQuery.length; j++)
		{
			if(sizeOfQuery[j]<minimumSize)
			{
				minimumSize = sizeOfQuery[j];
				minimumPosting = j;
			}
		}
		//We add the maximum posting list term at the start of our array list
		ref = queryTerms.get(minimumPosting);
		queryTerms.add(0,ref);
		queryTerms.remove(minimumPosting+1);
		int count = 1;
		int x = 0;
		//loop for traversing each item of reference list
		for(int i = 0; i<queryTerms.get(0).size(); i++)
		{
			count=1;
			//loop for traversing all the postings list in array list
			for(int j=1; j<queryTerms.size(); j++)
			{
				//loop for traversing the ids of each list
				for(int k=x; k<queryTerms.get(j).size(); k++)
				{
					comp2++;
					if(queryTerms.get(0).get(i).equals(queryTerms.get(j).get(k)))
					{
						count++;
						break;
					}
					else if(queryTerms.get(0).get(i)<queryTerms.get(j).get(k)){
						x = k;
						break;
					}
				}

			}
			/*take a counter which should be equal to number of postings list
			 * then only add the id to result
			 */
			if(count == queryTerms.size())
			{
				result.add(queryTerms.get(0).get(i));
			}
		}
		System.out.println("DaatAnd" + "\n" + query);
		o.write("\r\n" + "DaatAnd");
		o.write("\r\n" + query);
		System.out.print("Results: ");
		o.write("\r\n" + "Results: ");
		if(result.size() != 0){
			for(int i=0; i<result.size(); i++)
			{

				System.out.print(result.get(i) + " ");
				o.write(result.get(i) + " ");

			}
		}
		else
		{
			System.out.print("empty");
			o.write("empty");
		}
		System.out.println("\nNumber of documents in results: " + result.size());
		o.write("\r\n" + "Number of documents in results: " + result.size() + "\r\n");
		System.out.println("Number of comparisons: " + comp2);
		o.write("Number of comparisons: " + comp2 + "\r\n");
	}

}