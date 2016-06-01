package catdany.bfdist.server.report;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.mail.Session;

import org.apache.commons.io.FileUtils;

import catdany.bfdist.BFHelper;
import catdany.bfdist.log.BFLog;
import catdany.bfdist.server.BFServer;
import catdany.bfdist.server.ClientHandler;

public class Reporter implements Runnable
{
	public static File fileEmailInfo = new File("email.txt");
	public static File fileFolderArchive = new File("complogarchive");
	
	public static Mailer mailer = new Mailer();
	public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	public static final int MAX_EMAIL_ATTEMPTS = 5;
	
	public static ScheduledFuture<?> startOnSchedule(long delay, TimeUnit unit)
	{
		return scheduler.scheduleAtFixedRate(new Reporter(), delay, delay, unit);
	}
	
	/**
	 * Automatically handle e-mail report
	 */
	private static void reportEmail()
	{
		// Reading e-mail account info from file
		String emailAddressFrom;
		String emailPassword;
		String emailAddressTo;
		try
		{
			List<String> fileEmailInfoList = Files.readAllLines(fileEmailInfo.toPath());
			if (fileEmailInfoList.size() < 3)
				throw new IOException("E-mail info file contains less than 3 lines.");
			emailAddressFrom = fileEmailInfoList.get(0);
			emailPassword = fileEmailInfoList.get(1);
			emailAddressTo = fileEmailInfoList.get(2);
		}
		catch (IOException t)
		{
			BFLog.e("Unable to get e-mail login/pass from file");
			BFLog.t(t);
			return;
		}
		Session ses = mailer.getSession(emailAddressFrom, emailPassword);
		// Client dump
		String clientDump;
		try
		{
			clientDump = dumpActiveClients();
		}
		catch (ConcurrentModificationException t)
		{
			BFLog.e("You fucked up.");
			BFLog.t(t);
			mailer.sendTry(ses, emailAddressTo, emailAddressFrom, "DistComp Report :: You fucked up", BFHelper.writeException(t), null, MAX_EMAIL_ATTEMPTS);
			return;
		}
		// Zipping comp logs
		File zipFile;
		try
		{
			zipFile = performZipping();
		}
		catch (Exception t)
		{
			BFLog.e("Unable to zip and archive complogs.");
			BFLog.t(t);
			mailer.sendTry(ses, emailAddressTo, emailAddressFrom, "DistComp Report :: ZipError", clientDump + "\n" + BFHelper.writeException(t), null, MAX_EMAIL_ATTEMPTS);
			return;
		}
		if (FileUtils.sizeOf(zipFile) > 25*1024*1024-1)
		{
			mailer.sendTry(ses, emailAddressTo, emailAddressFrom, "DistComp Report :: ZipTooBig", clientDump, null, MAX_EMAIL_ATTEMPTS);
			return;
		}
		// Sending e-mail
		mailer.sendTry(ses, emailAddressTo, emailAddressFrom, "DistComp Report :: OK", "Successful DistComp report.\n\n" + clientDump, zipFile, MAX_EMAIL_ATTEMPTS);
	}
	
	private static File performZipping()
	{
		// Zipping and archiving comp logs
		if (!fileFolderArchive.exists() || !fileFolderArchive.isDirectory())
		{
			fileFolderArchive.mkdir();
		}
		File zipFile = new File(fileFolderArchive, BFHelper.dateFormatFile.format(new Date()) + ".zip");
		File[] toZip = new File(".").listFiles(new CompLogFilter());
		try
		{
			Zipper.zip(zipFile, toZip);
		}
		catch (IOException t)
		{
			throw new ReporterException(t, "Couldn't zip logs.");
		}
		return zipFile;
	}
	
	private static String dumpActiveClients()
	{
		ArrayList<ClientHandler> clients = BFServer.getServer().getClients();
		if (clients.isEmpty())
			return "No clients connected.";
		StringBuilder s = new StringBuilder(0);
		s.append("Client Dump:\n");
		//XXX: Double check: ConcurrentModificationException
		for (ClientHandler i : clients)
		{
			s.append(String.format("%s > Current: %s | Max: %s", i, i.current, i.max) + "\n");
		}
		return s.toString();
	}
	
	@Override
	public void run()
	{
		reportEmail();
	}
	
	public static class CompLogFilter implements FileFilter
	{
		@Override
		public boolean accept(File file)
		{
			return file.getName().startsWith("COMPLOG_");
		}
	}
}