import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import javax.swing.JFrame;

public class Parrot {
	TargetDataLine microphone;
	AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
	AudioFormat speakFormat = new AudioFormat(80000, 16, 1, true, true);
	int pieceLength = 17640;
	
	public static void main(String[] args) {
		JFrame frame = new JFrame("Parrot");
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setSize(500, 200);
		
		frame.setVisible(true);
		
		Parrot parrot = new Parrot();
		try {
			parrot.initialize();
			while (true) {
				System.out.println("Listening...");
				ArrayList<Short> sound = parrot.getSound((short)100);
				if (sound.size() > 1) {
					short[] toPlay = new short[sound.size()];
					for (int i = 0; i < toPlay.length; i++) {
						toPlay[i] = sound.get(i);
					}
					System.out.println("Speaking...");
					parrot.play(toPlay);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void initialize() throws LineUnavailableException {
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		microphone = (TargetDataLine) AudioSystem.getLine(info);
		microphone.open(format);
	}
	
	public byte[] record() {
		microphone.start();
		byte[] data = new byte[microphone.getBufferSize() / 5];
		pieceLength = data.length;
		microphone.read(data, 0, data.length);
		return data;
	}
	
	public void play(short[] data) throws LineUnavailableException {
		byte[] sound = this.ShortToByte(data);
		DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, speakFormat);
        SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        speakers.open(speakFormat);
        speakers.start();
        speakers.write(sound, 0, sound.length);
        speakers.drain();
        speakers.close();
	}
	
	public short[] listenOnce() {
		int factor = 3;
		byte[] input = new byte[pieceLength * factor];
		for (int i = 0; i < factor; i++) {
			byte[] once = this.record();
			for (int j = 0; j < once.length; j++) {
				input[(i * pieceLength) + j] = once[j];
			}
		}
		short[] data = this.ByteToShort(input);
		return data;
	}
	
	public short[] decideNoisiness(short threshold) {
		short[] data = this.listenOnce();
		boolean noisy = false;
		for (short value : data) {
			if (value > threshold) {
				noisy = true;
				break;
			}
		}
		if (noisy) {
			return data;
		}
		return new short[0];
	}
	
	public ArrayList<Short> getSound(short threshold) {
		ArrayList<Short> allData = new ArrayList<Short>();
		boolean recording = false;
		boolean done = false;
		while (!done) {
			short[] data = decideNoisiness(threshold);
			if (data.length > 0) {
				recording = true;
				for (short value : data) {
					allData.add(value);
				}
			}
			if (recording && data.length == 0) {
				recording = false;
				done = true;
			}
		}
		return allData;
	}
	
	public short[] ByteToShort(byte[] data) {
		short[] output = new short[data.length / 2];
		for (int i = 0; i < data.length; i += 2) {
			ByteBuffer bb = ByteBuffer.allocate(2);
			bb.order(ByteOrder.BIG_ENDIAN);
			bb.put(data[i]);
			bb.put(data[i + 1]);
			short shortVal = bb.getShort(0);
			output[i / 2] = shortVal;
		}
		return output;
	}
	
	public byte[] ShortToByte(short[] data) {
		byte[] output = new byte[data.length * 2];
		for (int i = 0; i < data.length; i++) {
			ByteBuffer bb = ByteBuffer.allocate(2);
			bb.putShort(data[i]);
			byte[] piece = bb.array();
			output[i * 2] = piece[0];
			output[i * 2 + 1] = piece[1];
		}
		return output;
	}
}
