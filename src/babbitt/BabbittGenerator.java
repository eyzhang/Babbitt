<<<<<<< HEAD
package babbitt;

/* 
 * Description: Generates a midi file of random music that kind of sounds like 
 * Milton Babbitt's work, of length n seconds.
 *  
 * Note: Sections of code denoted by "//****" comments are copied from
 * http://www.automatic-pilot.com/midifile.html by Karl Brown.
 *
 * @author Edward Zhang 
 * eyzhang@princeton.edu
 * Princeton University, Class of 2019
 */

import javax.sound.midi.*;

public class BabbittGenerator {
    // generate a random number between 0 and 1, uniformly weighted
    private static double weightedRandom0() {
        return Math.random();       
    }
    
    /*
    // generate a random number between 0 and 1 that's weighted towards 0.5
    private static double weightedRandom1() {
        double x = Math.random() - 0.5;
        double random = 0.5 - Math.sin(x) * Math.abs(x);
        return random;
    }
    */
    
    // generate a random note length that is long enough to be audible
    private static int randLength() {
        int LENGTH_MAX = 300;
        int LENGTH_MIN = 10;    
        int randLength = (int) (Math.random() * (LENGTH_MAX - LENGTH_MIN) + LENGTH_MIN);
        return randLength;
    }
    
    // generate a random velocity
    private static int randVeloc() {
        int VELOC_MAX = 300;
        int VELOC_MIN = 10;    
        int randVeloc = (int) (Math.random() * (VELOC_MAX - VELOC_MIN) + VELOC_MIN);
        return randVeloc;
    }
    
    // generate a random note
    private static int randNote() {
        // range of the piano keyboard
        int NOTE_MAX = 108;
        int NOTE_MIN = 9;
        
        boolean isIncluded = false;
        int randNote = (int) (weightedRandom0() * (NOTE_MAX - NOTE_MIN) + NOTE_MIN);
        
        
        return randNote;
    }
    
    // add a rest of random length to the given track
    private static void addRest(Track t) {
        int STATUS_NOTE_ON = 0x90;
        int STATUS_NOTE_OFF = 0x80;
        
        int length = randLength();
        int veloc = 0;
        int note = 0;
        
        try {
            // ON message
            ShortMessage mm = new ShortMessage();
            mm.setMessage(STATUS_NOTE_ON, note, veloc);
            MidiEvent me = new MidiEvent(mm, t.ticks() + 1);
            t.add(me);
            
            // OFF message
            mm = new ShortMessage();
            mm.setMessage(STATUS_NOTE_OFF, note, 30);
            me = new MidiEvent(mm, (long) (t.ticks() + length));
            t.add(me);
        } 
        catch (InvalidMidiDataException imde) {
            // System.out.println("addRest - Oops!");
        }
    }
    
    // add a random note of random length and velocity to the given track
    private static void addNote(Track t, boolean[] noteIsIncluded) {
        int STATUS_NOTE_ON = 0x90;
        int STATUS_NOTE_OFF = 0x80;
        
        int length = randLength();
        int veloc = randVeloc();
        int note = -1;
        
        boolean isIncluded = false;
        
        while (!isIncluded) {
            // generate random note
            note = randNote();
        
            // go through the inclusion array
            for (int i = 0; i < 12; i++) {
                // if the note is good to go, exit the loop
                if (noteIsIncluded[i] && ((note - i) % 12 == 0)) {
                    isIncluded = true;
                }
            }
        }

        
        try {
            // ON message
            ShortMessage mm = new ShortMessage();
            mm.setMessage(STATUS_NOTE_ON, note, veloc);
            MidiEvent me = new MidiEvent(mm, (long) (t.ticks() + 1));
            t.add(me);
            
            // OFF message
            mm = new ShortMessage();
            mm.setMessage(STATUS_NOTE_OFF, note, 30);
            me = new MidiEvent(mm, t.ticks() + length);
            t.add(me);
        } 
        catch (InvalidMidiDataException imde) {
            // System.out.println("addNote - Oops!");
        }
    }
    
    /**
     *
     * @param n Music length, in seconds 
     * @param noteIsIncluded indicates whether the pitch classes are to be included
     * @param tracks
     * @return MIDI sequence
     */
    public static Sequence generate(int n, boolean[] noteIsIncluded, int numTracks) {        
        try {
            // music length in seconds
            int musicLength = n * 200;
            
            //****  Create a new MIDI sequence with 24 ticks per beat and specified number of tracks
            Sequence seq = new Sequence(javax.sound.midi.Sequence.PPQ, 24, numTracks);
            
            // Obtain all MIDI tracks from the sequence and set up each track
            Track[] t = new Track[numTracks];
            for (int i = 0; i < numTracks; i++) {
                t[i] = seq.createTrack();
                
                //****  General MIDI sysex -- turn on General MIDI sound set 
                byte[] b = {(byte)0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte)0xF7};
                SysexMessage sm = new SysexMessage();
                sm.setMessage(b, 6);
                MidiEvent me = new MidiEvent(sm,(long)0);
                t[i].add(me);
                
                //****  set tempo (meta event)  
                MetaMessage mt = new MetaMessage();
                byte[] bt = {0x02, (byte)0x00, 0x00};
                mt.setMessage(0x51 ,bt, 3);
                me = new MidiEvent(mt,(long)0);
                t[i].add(me);
                
                //****  set track name (meta event)  
                mt = new MetaMessage();
                String TrackName = "midifile track";
                mt.setMessage(0x03 ,TrackName.getBytes(), TrackName.length());
                me = new MidiEvent(mt,(long)0);
                t[i].add(me);
                
                //****  set omni on  
                ShortMessage mm = new ShortMessage();
                mm.setMessage(0xB0, 0x7D,0x00);
                me = new MidiEvent(mm,(long)0);
                t[i].add(me);
                
                //****  set poly on  
                mm = new ShortMessage();
                mm.setMessage(0xB0, 0x7F,0x00);
                me = new MidiEvent(mm,(long)0);
                t[i].add(me);
                
                //****  set instrument to Piano  
                mm = new ShortMessage();
                mm.setMessage(0xC0, 0x00, 0x00);
                me = new MidiEvent(mm,(long)0);
                t[i].add(me);
            }
            
            // start adding notes to each track
            for (int i = 0; i < numTracks; i++) {
                while (t[i].ticks() < musicLength) {
                    // randomly choose between adding rests and notes
                    double nextIsNote = weightedRandom0();
                    if (nextIsNote <= 0.4) {
                        // add a rest
                        addRest(t[i]);
                    }
                    else {
                        // add a note
                        addNote(t[i], noteIsIncluded);
                    } 
                }
            }
            
            /*
            // write the MIDI sequence to a MIDI file
            File file = new File(filename);
            MidiSystem.write(seq, 1, file);
            */
            
            return seq;
        }
        catch(InvalidMidiDataException e)  {
            System.out.println("Exception caught " + e.toString());
            return null;
        }
        
    }
=======
package babbitt;

/* 
 * Description: Generates a midi file of random music that kind of sounds like 
 * Milton Babbitt's work, of length n seconds.
 *  
 * Note: Sections of code denoted by "//****" comments are copied from
 * http://www.automatic-pilot.com/midifile.html by Karl Brown.
 *
 * @author Edward Zhang 
 * eyzhang@princeton.edu
 * Princeton University, Class of 2019
 */

import javax.sound.midi.*;

public class BabbittGenerator {
    // generate a random number between 0 and 1, uniformly weighted
    private static double weightedRandom0() {
        return Math.random();       
    }
    
    // generate a random number between 0 and 1 that's weighted towards 0.5
    private static double weightedRandom1() {
        double x = Math.random() - 0.5;
        double random = 0.5 - Math.sin(x) * Math.abs(x);
        return random;
    }
    
    // generate a random note length that is long enough to be audible
    private static int randLength() {
        int LENGTH_MAX = 300;
        int LENGTH_MIN = 10;    
        int randLength = (int) (Math.random() * (LENGTH_MAX - LENGTH_MIN) + LENGTH_MIN);
        return randLength;
    }
    
    // generate a random velocity
    private static int randVeloc() {
        int VELOC_MAX = 300;
        int VELOC_MIN = 10;    
        int randVeloc = (int) (Math.random() * (VELOC_MAX - VELOC_MIN) + VELOC_MIN);
        return randVeloc;
    }
    
    // generate a random note
    private static int randNote() {
        int NOTE_MAX = 120;
        int NOTE_MIN = 0;
        
        boolean isIncluded = false;
        int randNote = (int) (weightedRandom0() * (NOTE_MAX - NOTE_MIN) + NOTE_MIN);
        
        
        return randNote;
    }
    
    // add a rest of random length to the given track
    private static void addRest(Track t) {
        int STATUS_NOTE_ON = 0x90;
        int STATUS_NOTE_OFF = 0x80;
        
        int length = randLength();
        int veloc = 0;
        int note = 0;
        
        try {
            // ON message
            ShortMessage mm = new ShortMessage();
            mm.setMessage(STATUS_NOTE_ON, note, veloc);
            MidiEvent me = new MidiEvent(mm, t.ticks() + 1);
            t.add(me);
            
            // OFF message
            mm = new ShortMessage();
            mm.setMessage(STATUS_NOTE_OFF, note, 30);
            me = new MidiEvent(mm, (long) (t.ticks() + length));
            t.add(me);
        } 
        catch (InvalidMidiDataException imde) {
            // System.out.println("addRest - Oops!");
        }
    }
    
    // add a random note of random length and velocity to the given track
    private static void addNote(Track t, boolean[] noteIsIncluded) {
        int STATUS_NOTE_ON = 0x90;
        int STATUS_NOTE_OFF = 0x80;
        
        int length = randLength();
        int veloc = randVeloc();
        int note = -1;
        
        boolean isIncluded = false;
        
        while (!isIncluded) {
            // generate random note
            note = randNote();
        
            // go through the inclusion array
            for (int i = 0; i < 12; i++) {
                // if the note is included, exit the loop
                if (noteIsIncluded[i] && ((note - i) % 12 == 0)) {
                    isIncluded = true;
                }
            }
        }

        
        try {
            // ON message
            ShortMessage mm = new ShortMessage();
            mm.setMessage(STATUS_NOTE_ON, note, veloc);
            MidiEvent me = new MidiEvent(mm, (long) (t.ticks() + 1));
            t.add(me);
            
            // OFF message
            mm = new ShortMessage();
            mm.setMessage(STATUS_NOTE_OFF, note, 30);
            me = new MidiEvent(mm, t.ticks() + length);
            t.add(me);
        } 
        catch (InvalidMidiDataException imde) {
            // System.out.println("addNote - Oops!");
        }
    }
    
    /**
     *
     * @param n Music length, in seconds
     * @param pitchClasses Set of pitches, e.g. "00 01 02 03 04 05 06 07 08 09 10 11" for full chromatic scale 
     * @return MIDI sequence
     */
    public static Sequence generate(int n, String pitchClasses) {        
        try {
            // music length in seconds
            int musicLength = n * 200;
            
            //  boolean array of pitch classes
            boolean[] noteIsIncluded = new boolean[12];
            for (int i = 0; i < 10; i++) {
                boolean contains = pitchClasses.contains("0" + i);
                if (contains) {
                    noteIsIncluded[i] = true;
                }
            }
            for (int i = 11; i < 12; i++) {
                boolean contains = pitchClasses.contains(String.valueOf(i));
                if (contains) {
                    noteIsIncluded[i] = true;
                }
            }
            
            //****  Create a new MIDI sequence with 24 ticks per beat and 10 tracks
            int NUM_TRACKS = 10;
            Sequence seq = new Sequence(javax.sound.midi.Sequence.PPQ, 24, NUM_TRACKS);
            
            // Obtain all MIDI tracks from the sequence and set up each track
            Track[] t = new Track[NUM_TRACKS];
            for (int i = 0; i < NUM_TRACKS; i++) {
                t[i] = seq.createTrack();
                
                //****  General MIDI sysex -- turn on General MIDI sound set 
                byte[] b = {(byte)0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte)0xF7};
                SysexMessage sm = new SysexMessage();
                sm.setMessage(b, 6);
                MidiEvent me = new MidiEvent(sm,(long)0);
                t[i].add(me);
                
                //****  set tempo (meta event)  
                MetaMessage mt = new MetaMessage();
                byte[] bt = {0x02, (byte)0x00, 0x00};
                mt.setMessage(0x51 ,bt, 3);
                me = new MidiEvent(mt,(long)0);
                t[i].add(me);
                
                //****  set track name (meta event)  
                mt = new MetaMessage();
                String TrackName = "midifile track";
                mt.setMessage(0x03 ,TrackName.getBytes(), TrackName.length());
                me = new MidiEvent(mt,(long)0);
                t[i].add(me);
                
                //****  set omni on  
                ShortMessage mm = new ShortMessage();
                mm.setMessage(0xB0, 0x7D,0x00);
                me = new MidiEvent(mm,(long)0);
                t[i].add(me);
                
                //****  set poly on  
                mm = new ShortMessage();
                mm.setMessage(0xB0, 0x7F,0x00);
                me = new MidiEvent(mm,(long)0);
                t[i].add(me);
                
                //****  set instrument to Piano  
                mm = new ShortMessage();
                mm.setMessage(0xC0, 0x00, 0x00);
                me = new MidiEvent(mm,(long)0);
                t[i].add(me);
            }
            
            // start adding notes to each track
            for (int i = 0; i < NUM_TRACKS; i++) {
                while (t[i].ticks() < musicLength) {
                    // randomly choose between adding rests and notes
                    double nextIsNote = weightedRandom0();
                    if (nextIsNote <= 0.4) {
                        // add a rest
                        addRest(t[i]);
                    }
                    else {
                        // add a note
                        addNote(t[i], noteIsIncluded);
                    } 
                }
            }
            
            /*
            // write the MIDI sequence to a MIDI file
            File file = new File(filename);
            MidiSystem.write(seq, 1, file);
            */
            
            return seq;
        }
        catch(InvalidMidiDataException e)  {
            System.out.println("Exception caught " + e.toString());
            return null;
        }
        
    }
>>>>>>> origin/master
}