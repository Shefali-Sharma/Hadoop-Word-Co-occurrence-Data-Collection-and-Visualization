import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;



public class Pairs{

  public static class PairOccurenceMapper extends Mapper<LongWritable, Text, WordPair, IntWritable>{
	
	private static final WordPair pair = new Pairs.WordPair();  
    private final static IntWritable writable_val = new IntWritable(1);

    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
    	
    	
        String[] tokens = value.toString().split("\\s+");
        int ncount = context.getConfiguration().getInt("co-occurring word", tokens.length);
        
        if (tokens.length > 1) {
          for (int i = 0; i < tokens.length; i++) {
        	  String token_i = tokens[i].toString().toLowerCase();
        	  pair.setKey(token_i);
             
        	  int beg = (i - ncount < 0) ? 0 : i - ncount;
 	          int end = (i + ncount >= tokens.length) ? tokens.length - 1 : i + ncount;
             
             for (int j = beg; j <= end; j++) {
                  if (j == i) continue;
                  
                  String tokenized_word = tokens[j].toString().toLowerCase();
                  pair.setcooccur_word(tokenized_word);
                  context.write(pair, writable_val);
              }
          }
         }
    }
    
  }

  public static class PairCountReducer extends Reducer<WordPair,IntWritable,WordPair,IntWritable> {
    private IntWritable result = new IntWritable();

    public void reduce(WordPair key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
    	
      int count = 0;
      
      for (IntWritable val : values) {
        count += val.get();
      }
      result.set(count);
      context.write(key, result);
    }
  }
  
  public static class WordPair implements Writable,WritableComparable<WordPair> {

		private Text key;
	    private Text cooccur_word;
	    
	    public WordPair() {
	        this.key = new Text();
	        this.cooccur_word = new Text();
	    }

	    public WordPair(String word, String cooccur_word) {
	        this(new Text(word),new Text(cooccur_word));
	    }
	    
	    public WordPair(Text key, Text cooccur_word) {
	        this.key = key;
	        this.cooccur_word = cooccur_word;
	    }
	    
	    public void setcooccur_word(String cooccur_word){
	    	this.cooccur_word = new Text (cooccur_word);
	    }

		public void setKey(String key) {
			// TODO Auto-generated method stub
			this.key = new Text(key);
		}
		
		public Text getKey() {
	        return key;
	    }

	    public Text getcooccur_word() {
	        return cooccur_word;
	    }
		
		//@Override
	    public int compareTo(WordPair other) {
	        int returnVal = this.key.compareTo(other.getKey());
	        if(returnVal != 0){
	            return returnVal;
	        }
	        if(this.cooccur_word.toString().equals("*")){
	            return -1;
	        }else if(other.getcooccur_word().toString().equals("*")){
	            return 1;
	        }
	        return this.cooccur_word.compareTo(other.getcooccur_word());
	    }

	    @Override
	    public String toString() {
            return "{text:"+key+"-"+cooccur_word+"}";
	    }
		
		public static WordPair read(DataInput in) throws IOException {
	        WordPair wordPair = new WordPair();
	        wordPair.readFields(in);
	        return wordPair;
	    }

	    //@Override
	    public void write(DataOutput out) throws IOException {
	        key.write(out);
	        cooccur_word.write(out);
	    }

	    //@Override
	    public void readFields(DataInput in) throws IOException {
	        key.readFields(in);
	        cooccur_word.readFields(in);
	    }


	}


  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "Pairs");
    job.setJarByClass(Pairs.class);
    job.setMapperClass(PairOccurenceMapper.class);
    job.setCombinerClass(PairCountReducer.class);
    job.setReducerClass(PairCountReducer.class);
    job.setOutputKeyClass(Pairs.WordPair.class);
    job.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
