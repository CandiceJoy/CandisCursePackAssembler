import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class PackAssembler
{
	public static final int BUFFER = 2048;

	public static final String EXTRACTED_DIR = "." + File.separator + "extracted";
	public static final String PACK_DIR = "." + File.separator + "pack";

	private static final PackAssemblerGUI gui = new PackAssemblerGUI();
	private static final ArrayList<String> errors = new ArrayList<String>();

	public static void main( String[] str )
	{
		EventQueue.invokeLater( new Runnable()
		{
			public void run()
			{
				try
				{
					gui.getFrame().setVisible( true );
				}
				catch( Exception e )
				{
					e.printStackTrace();
					errors.add( e.toString() );
				}
			}
		} );

		gui.addLine( "Instructions: Download the appropriate pack's zip file from Curse and choose it in the file choosing window." );
		gui.addLine( "This application will then assemble the pack for you." );

		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Curse zip files", "zip" );
		chooser.setFileFilter( filter );
		int returnVal = chooser.showOpenDialog( null );
		if( returnVal == JFileChooser.APPROVE_OPTION )
		{
			gui.addLine( "You chose to open this file: " +
					chooser.getSelectedFile().getPath() );
		}

		String path = chooser.getSelectedFile().getPath();
		File extracted_directory = new File( EXTRACTED_DIR );
		File pack_directory = new File( PACK_DIR );

		if( extracted_directory.exists() )
		{
			deleteDirectory( extracted_directory );
		}

		if( pack_directory.exists() )
		{
			deleteDirectory( pack_directory );
		}

		extracted_directory.mkdir();
		pack_directory.mkdir();

		gui.addString( "Unzipping..." );
		unzipFunction( "extracted", path );
		gui.addLine( "done" );

		try
		{
			Files.move( Paths.get( EXTRACTED_DIR + File.separator + "Overrides" + File.separator + "mods" ), Paths.get( PACK_DIR + File.separator + "mods" ) );
			Files.move( Paths.get( EXTRACTED_DIR + File.separator + "Overrides" + File.separator + "config" ), Paths.get( PACK_DIR + File.separator + "config" ) );
			Files.move( Paths.get( EXTRACTED_DIR + File.separator + "Overrides" + File.separator + "scripts" ), Paths.get( PACK_DIR + File.separator + "scripts" ) );
		}
		catch( IOException e )
		{
			e.printStackTrace();
			errors.add( e.toString() );
		}

		String text = null;
		try
		{
			text = new Scanner( new File( "extracted" + File.separator + "manifest.json" ) ).useDelimiter( "\\A" ).next();
		}
		catch( FileNotFoundException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			errors.add( e.toString() );
		}

		if( text == null )
		{
			gui.addLine( "Error in opening manifest" );
			System.exit( 0 );
		}

		JSONObject o = new JSONObject( text );
		JSONArray arr = o.getJSONArray( "files" );

		for( int x = 0; x < arr.length(); x++ )
		{
			JSONObject sub_arr = arr.getJSONObject( x );

			int project_id = sub_arr.getInt( "projectID" );
			int file_id = sub_arr.getInt( "fileID" );
			String project_name = getProjectName( project_id );
			String file_url = "http://minecraft.curseforge.com/mc-mods/" + project_id + "-" + project_name + "/files/" + file_id + "/download";

			try
			{
				downloadFile( file_url, PACK_DIR + File.separator + "mods" );
			}
			catch( IOException e )
			{
				e.printStackTrace();
				errors.add( e.toString() );
			}

			gui.addLine( ( x + 1 ) + "/" + ( arr.length() ) + " (" + Math.round( (float) ( (float) ( x + 1 ) / (float) ( arr.length() ) ) * 100 ) + "%)" );
		}

		if( errors.size() > 0 )
		{
			gui.addLine( "!!!ERRORS!!!" );

			for( String error : errors )
			{
				gui.addLine( error );
			}

			gui.addLine( "!!!END ERRORS!!!" );
			gui.addLine( "There were errors.  Your pack may not be usable in its current state." );
		}
		else
		{
			gui.addLine( "All done!  If there were no errors, your pack should now be assembled in the pack folder." );
		}
	}

	private static boolean deleteDirectory( File path )
	{
		if( path.exists() )
		{
			File[] files = path.listFiles();
			for( int i = 0; i < files.length; i++ )
			{
				if( files[i].isDirectory() )
				{
					deleteDirectory( files[i] );
				}
				else
				{
					files[i].delete();
				}
			}
		}
		return ( path.delete() );
	}

	public static void downloadFile( String fileURL, String saveDir )
			throws IOException
	{
		URL url = new URL( fileURL );
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		int responseCode = httpConn.getResponseCode();

		// always check HTTP response code first
		if( responseCode == HttpURLConnection.HTTP_OK )
		{
			String fileName = "";
			String disposition = httpConn.getHeaderField( "Content-Disposition" );
			//String contentType = httpConn.getContentType();
			//int contentLength = httpConn.getContentLength();

			if( disposition != null )
			{
				// extracts file name from header field
				int index = disposition.indexOf( "filename=" );
				if( index > 0 )
				{
					fileName = disposition.substring( index + 10,
							disposition.length() - 1 );
				}
			}
			else
			{
				// extracts file name from URL
				fileName = fileURL.substring( fileURL.lastIndexOf( "/" ) + 1,
						fileURL.length() );
			}

			fileName = httpConn.getURL().toString().substring( httpConn.getURL().toString().lastIndexOf( "/" ) + 1 ).replace( "%20", " " );

			gui.addString( "Downloading " + fileName + "..." );

			// opens input stream from the HTTP connection
			InputStream inputStream = httpConn.getInputStream();
			String saveFilePath = saveDir + File.separator + fileName;

			// opens an output stream to save into file
			FileOutputStream outputStream = new FileOutputStream( saveFilePath );

			int bytesRead = -1;
			byte[] buffer = new byte[BUFFER];
			while( ( bytesRead = inputStream.read( buffer ) ) != -1 )
			{
				outputStream.write( buffer, 0, bytesRead );
			}

			outputStream.close();
			inputStream.close();

			gui.addLine( "done" );
		}
		else
		{
			gui.addLine( "No file to download. Server replied HTTP code: " + responseCode );
			errors.add( "Error downloading file: " + fileURL );
		}
		httpConn.disconnect();
	}

	private static String getProjectName( int project_id )
	{
		String project_url = "http://minecraft.curseforge.com/mc-mods/" + project_id;
		String project_name = "";

		try
		{
			URL oracle = new URL( project_url );
			URLConnection yc = oracle.openConnection();
			BufferedReader in = new BufferedReader( new InputStreamReader(
					yc.getInputStream() ) );
			String inputLine;
			boolean found = false;

			while( ( inputLine = in.readLine() ) != null )
			{
				Pattern p1 = Pattern.compile( "\"/mc-mods/" + project_id + "-[a-zA-Z0-9-]+\"" );
				Matcher m1 = p1.matcher( inputLine );

				Pattern p2 = Pattern.compile( "\"/mc-mods/" + project_id + "-[a-zA-Z0-9-]+-" + project_id + "\"" );
				Matcher m2 = p2.matcher( inputLine );

				while( !found && m2.find() )
				{
					String partial_url = m2.group();
					found = true;
					//"/mc-mods/32274-journeymap-32274"

					project_name = partial_url.substring( partial_url.indexOf( "-", 8 ) + 1, partial_url.lastIndexOf( "-" ) );
				}

				while( !found && m1.find() )
				{
					String partial_url = m1.group();
					found = true;
					///mc-mods/51195-railcraft

					project_name = partial_url.substring( partial_url.indexOf( "-", 8 ) + 1, partial_url.length() - 1 );
				}


				if( found )
				{
					return project_name;
				}
			}
		}
		catch( MalformedURLException e )
		{
			e.printStackTrace();
			errors.add( e.toString() );
		}
		catch( IOException e )
		{
			e.printStackTrace();
			errors.add( e.toString() );
		}

		return null;
	}

	private static void unzipFunction( String destinationFolder, String zipFile )
	{
		File directory = new File( destinationFolder );

		// if the output directory doesn't exist, create it
		if( !directory.exists() )
		{
			directory.mkdirs();
		}

		// buffer for read and write data to file
		byte[] buffer = new byte[2048];

		try
		{
			FileInputStream fInput = new FileInputStream( zipFile );
			ZipInputStream zipInput = new ZipInputStream( fInput );

			ZipEntry entry = zipInput.getNextEntry();

			while( entry != null )
			{
				String entryName = entry.getName();
				File file = new File( destinationFolder + File.separator + entryName );

				//gui.addLine("Unzip file " + entryName + " to " + file.getAbsolutePath());

				// create the directories of the zip directory
				if( entry.isDirectory() )
				{
					File newDir = new File( file.getAbsolutePath() );
					if( !newDir.exists() )
					{
						boolean success = newDir.mkdirs();
						if( success == false )
						{
							gui.addLine( "Problem creating Folder" );
							errors.add( "Problem creating Folder" );
						}
					}
				}
				else
				{
					FileOutputStream fOutput = new FileOutputStream( file );
					int count = 0;
					while( ( count = zipInput.read( buffer ) ) > 0 )
					{
						// write 'count' bytes to the file output stream
						fOutput.write( buffer, 0, count );
					}
					fOutput.close();
				}
				// close ZipEntry and take the next one
				zipInput.closeEntry();
				entry = zipInput.getNextEntry();
			}

			// close the last ZipEntry
			zipInput.closeEntry();

			zipInput.close();
			fInput.close();
		}
		catch( IOException e )
		{
			e.printStackTrace();
			errors.add( e.toString() );
		}
	}
}