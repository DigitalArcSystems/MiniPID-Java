package com.stormbots;

public class Main {

	/**
	 * @param args Any arguments passed from stdin
	 */
	public static void main(String[] args) {
		MiniPID miniPID;
		CoilAndTesterModel model = new CoilAndTesterModel();
		miniPID = new MiniPID(2, 0, 0);
		miniPID.setOutputLimits(0, 300);
		miniPID.setOutputRampRate(20);
		//miniPID.setMaxIOutput(2);
		//miniPID.setOutputRampRate(3);
		//miniPID.setOutputFilter(.3);
		miniPID.setSetpointRange(300);


		model.start();

		double target=7000;
		
		double actual=0;
		double output=0;
		
		miniPID.setSetpoint(0);
		miniPID.setSetpoint(target);
		
		System.err.printf("Target\tActual\tOutput\tError\n");
		//System.err.printf("Output\tP\tI\tD\n");

		// Position based test code
		for (int i = 0; i < 10000000; i++){
			
			//if(i==50)miniPID.setI(.05);
//
//			if (i == 600)
//				target = 50;
				
			//if(i==75)target=(100);
			//if(i>50 && i%4==0)target=target+(Math.random()-.5)*50;
			
			output = miniPID.getOutput(actual, target);
			model.setDialPosition(output);
			actual = model.getCurrentVoltage();
			
			//System.out.println("=========================="); 
			//System.out.printf("Current: %3.2f , Actual: %3.2f, Error: %3.2f\n",actual, output, (target-actual));
			System.err.printf("%3.2f\t%3.2f\t%3.2f\t%3.2f\n", target, actual, output, (target-actual));
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//if(i>80 && i%5==0)actual+=(Math.random()-.5)*20;
		}		
	}
}
