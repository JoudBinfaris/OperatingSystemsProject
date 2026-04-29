package proj;

import java.util.LinkedList;
import java.util.Queue;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//shared job queue between thread 1 and thread 2
        Queue<PCB> jobQueue = new LinkedList<>();

        //start thread 1 (file reader)
        Thread fileReader = new Thread(new FileReaderThread("src/proj/job.txt", jobQueue));
        fileReader.start();


	}

}
