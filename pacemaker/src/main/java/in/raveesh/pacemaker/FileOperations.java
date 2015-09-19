package in.raveesh.pacemaker;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileOperations
{

    public static final String FILE_NAME = "heartbeat_data";
    private static final String filePath = Environment.getExternalStorageDirectory() + "/." + FILE_NAME + ".txt";
    private static File file = new File(filePath); //File object for our path
    private static int fileDelay; //Delay as set in file
    private static String filePackage; //The package as set in file
    private static long fileLastModified; //Last modified date of file

    /**
     * Writes to the heartbeat_data file
     * note: Overwrites existing data in the file
     *
     * @param packageName The name of the package which is writing to the file
     * @param delay       Gap between heartbeats in minutes
     */
    public static Boolean write(String packageName, int delay)
    {
        try
        {
            Log.i("Heartbeater", "Writing to file : " + FILE_NAME);

            // If file does not exists, then create it
            if (!file.exists())
            {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            /*  Use below to append (incase we want all the records ->
                FileWriter fw = new FileWriter(file.getAbsoluteFile(), true); */
            BufferedWriter bw = new BufferedWriter(fw);
            bw.append(delay + "\t" + packageName);
            bw.newLine();
            bw.close();
            fileLastModified = file.lastModified();
            read();
            return true;

        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Reads from the heartbeat_data file
     * Also responsible for setting local variables based on file input
     */
    public static String read()
    {
        Log.i("Heartbeater", "Reading from file : " + FILE_NAME);
        BufferedReader br;
        String response;

        try
        {
            StringBuffer output = new StringBuffer();

            br = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = br.readLine()) != null)
            {
                output.append(line);
            }
            response = output.toString();

        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }

        //Set the local variables once we get data from file
        String[] split = response.split("\\s+");
        setFileDelay(Integer.parseInt(split[0]));
        setFilePackage(split[1]);
        return response;

    }

    /**
     * Check whether file heartbeat_data exists
     */
    public static boolean exists()
    {
        Log.i("Heartbeater", "Checking if file exists : " + FILE_NAME);
        return file.exists();
    }

    private static void setFileDelay(int delay)
    {
        fileDelay = delay;
    }

    private static void setFileLastModified(long time)
    {
        fileLastModified = time;
    }

    private static void setFilePackage(String pckg)
    {
        filePackage = pckg;
    }

    public static int getFileDelay()
    {
        read();
        return fileDelay;
    }

    public static String getFilePackage()
    {
        read();
        return filePackage;
    }

    /**
     * Check whether file heartbeat_data has been modified
     * By comparing with local variable fileLastModified
     */
    public static boolean isFileModified()
    {
        Log.i("Heartbeater", "Checking for file modifications : " + FILE_NAME);
        Long timeFromFile = file.lastModified();
        if (timeFromFile > fileLastModified)
        {
            setFileLastModified(timeFromFile);
            return true;
        }
        return false;
    }


}
