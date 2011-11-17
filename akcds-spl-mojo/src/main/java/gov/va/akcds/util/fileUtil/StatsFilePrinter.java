package gov.va.akcds.util.fileUtil;

import gov.va.akcds.util.ConsoleUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StatsFilePrinter
{
	private BufferedWriter writer_;
	private String eol_;
	private String delimiter_;
	
	public StatsFilePrinter(String[] columnNames, String delimiter, String eol, File output, String description) throws IOException
	{
		eol_ = eol;
		delimiter_ = delimiter;
		ConsoleUtil.println("See: " + output + " for " + description);
		
		writer_ = new BufferedWriter(new FileWriter(output));
		
		for (int i = 0; i < columnNames.length; i++)
		{
			writer_.write(columnNames[i]);
			if (i < columnNames.length - 1)
			{
				writer_.write(delimiter);
			}
		}
		writer_.write(eol);
	}
	
	public void addLine(String[] columnData) throws IOException
	{
		for (int i = 0; i < columnData.length; i++)
		{
			writer_.write(columnData[i]);
			if (i < columnData.length - 1)
			{
				writer_.write(delimiter_);
			}
		}
		writer_.write(eol_);
	}
	
	public void close() throws IOException
	{
		writer_.flush();
		writer_.close();
	}
}
