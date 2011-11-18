package com.excilys.ebi.gatling.recorder.ui.component;

import static com.excilys.ebi.gatling.recorder.ui.Constants.GATLING_REQUEST_BODIES_DIRECTORY_NAME;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;

import com.excilys.ebi.gatling.recorder.configuration.Configuration;
import com.excilys.ebi.gatling.recorder.http.GatlingHttpProxy;
import com.excilys.ebi.gatling.recorder.http.event.ProxyEvent;
import com.excilys.ebi.gatling.recorder.http.event.ProxyListener;
import com.excilys.ebi.gatling.recorder.http.event.TagEvent;
import com.excilys.ebi.gatling.recorder.ui.enumeration.Filter;
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.ResultType;
import com.excilys.ebi.gatling.recorder.ui.event.HttpEvent;

@SuppressWarnings("serial")
public class RunningFrame extends JFrame implements ProxyListener {

	private Configuration configuration;
	private GatlingHttpProxy proxy;
	private Date startDate;

	private JTextField txtTag = new JTextField(10);
	private JButton btnTag = new JButton("Set");
	private DefaultListModel listElements = new DefaultListModel();
	private JList listExecutedRequests = new JList(listElements);

	private List<EventObject> listRequests = new ArrayList<EventObject>();
	private String protocol;
	private String domain;
	private int port;
	private String urlBase = null;
	private String urlBaseString = null;
	private TreeMap<String, String> urls = new TreeMap<String, String>();
	private TreeMap<String, Map<String, String>> headers = new TreeMap<String, Map<String, String>>();

	public RunningFrame() {

		/* Initialization of the frame */
		setTitle("Recorder running...");
		setMinimumSize(new Dimension(660, 480));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);

		/* Declaration & initialization of components */
		JButton btnClear = new JButton("Clear");
		final JButton btnStop = new JButton("Stop !");
		btnStop.setSize(120, 30);

		JScrollPane panelFilters = new JScrollPane(listExecutedRequests);
		panelFilters.setPreferredSize(new Dimension(300, 100));

		final TextAreaPanel stringRequest = new TextAreaPanel("Request:");
		stringRequest.setPreferredSize(new Dimension(330, 100));
		final TextAreaPanel stringResponse = new TextAreaPanel("Response:");
		final TextAreaPanel stringRequestBody = new TextAreaPanel("Request Body:");
		JSplitPane requestResponsePane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(stringRequest), new JScrollPane(stringResponse));
		final JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, requestResponsePane, stringRequestBody);

		/* Layout */
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 5, 0, 0);

		gbc.gridx = 0;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.gridy = 0;
		add(new JLabel("Tag :"), gbc);

		gbc.gridx = 1;
		add(txtTag, gbc);

		gbc.gridx = 2;
		gbc.weightx = 0.5;
		add(btnTag, gbc);

		gbc.gridx = 3;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.weightx = 0.25;
		add(btnClear, gbc);

		gbc.gridx = 4;
		gbc.anchor = GridBagConstraints.LINE_END;
		add(btnStop, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1;
		gbc.weighty = 0.25;
		gbc.fill = GridBagConstraints.BOTH;
		add(panelFilters, gbc);

		gbc.gridy = 3;
		gbc.weightx = 1;
		gbc.weighty = 0.75;
		gbc.fill = GridBagConstraints.BOTH;
		add(sp, gbc);

		/* Listeners */
		btnTag.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (!txtTag.getText().equals(EMPTY)) {
					listElements.addElement("Tag ! " + txtTag.getText());
					listExecutedRequests.ensureIndexIsVisible(listElements.getSize() - 1);
					listRequests.add(new TagEvent(txtTag.getText()));
					txtTag.setText(EMPTY);
				}
			}
		});

		listExecutedRequests.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (listExecutedRequests.getSelectedIndex() >= 0) {
					Object obj = listRequests.get(listExecutedRequests.getSelectedIndex());
					if (obj instanceof HttpEvent) {
						HttpEvent request = (HttpEvent) obj;
						stringRequest.txt.setText(request.getStringRequest());
						stringResponse.txt.setText(request.getStringResponse());
						stringRequestBody.txt.setText(request.getContent());
					} else {
						stringRequest.txt.setText(EMPTY);
						stringResponse.txt.setText(EMPTY);
						stringRequestBody.txt.setText(EMPTY);
					}
				}
			}
		});

		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				listElements.removeAllElements();
				stringRequest.txt.setText(EMPTY);
				stringRequestBody.txt.setText(EMPTY);
				stringResponse.txt.setText(EMPTY);
				listRequests.clear();
				urls.clear();
				headers.clear();
				protocol = null;
				domain = null;
				port = -1;
				urlBase = null;
				urlBaseString = null;
			}
		});

		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
	}

	public void start(Configuration configuration) {
		this.configuration = configuration;
		startDate = new Date();
		proxy = new GatlingHttpProxy(configuration.getProxyPort(), configuration.getOutgoingProxyHost(), configuration.getOutgoingProxyPort());
		proxy.addProxyListener(this);
		proxy.start();
	}

	private void stop() {
		saveScenario();
		proxy.shutdown();
		Controller.getInstance().onStop();
	}

	public void onHttpRequest(ProxyEvent e) {

	}

	public void onHttpResponse(ProxyEvent e) {
		if (addRequest(e))
			processRequest(e);
	}

	private boolean addRequest(ProxyEvent e) {
		HttpRequest request = e.getOriginalRequest();
		boolean add = true;
		URI uri = null;
		try {
			uri = new URI(request.getUri());
		} catch (URISyntaxException ex) {
			System.err.println("Can't create URI from request uri (" + request.getUri() + ")" + ex.getStackTrace());
			// FIXME error handling
			return false;
		}

		if (configuration.getFilterType() == FilterType.All) {
			Pattern pattern;
			Matcher matcher;
			if (configuration.getFilter() == Filter.Java) {

				if (configuration.getFilterType() == FilterType.Only) {
					for (String f : configuration.getFilters()) {
						pattern = Pattern.compile(f);
						matcher = pattern.matcher(uri.toString());
						if (!matcher.find())
							add = false;
					}
				} else if (configuration.getFilterType() == FilterType.Except) {
					for (String f : configuration.getFilters()) {
						pattern = Pattern.compile(f);
						matcher = pattern.matcher(uri.toString());
						if (matcher.find())
							add = false;
					}
				}
			} else if (configuration.getFilter() == Filter.Ant) {
				if (configuration.getFilterType() == FilterType.Only) {
					for (String f : configuration.getFilters()) {
						pattern = Pattern.compile(toRegexp(f));
						matcher = pattern.matcher(uri.getPath());
						if (!matcher.find())
							add = false;
					}
				} else if (configuration.getFilterType() == FilterType.Except) {
					for (String f : configuration.getFilters()) {
						pattern = Pattern.compile(toRegexp(f));
						matcher = pattern.matcher(uri.getPath());
						if (matcher.find())
							add = false;
					}
				}
			}
		}
		return add;
	}

	private void processRequest(ProxyEvent e) {

		HttpRequest request = e.getOriginalRequest();

		URI uri = null;
		try {
			uri = new URI(request.getUri());
		} catch (URISyntaxException ex) {
			System.err.println("Can't create URI from request uri (" + request.getUri() + ")" + ex.getStackTrace());
		}

		listElements.addElement(request.getMethod() + " | " + request.getUri());
		listExecutedRequests.ensureIndexIsVisible(listElements.getSize() - 1);

		int id = listRequests.size() + 1;
		e.setId(id);

		/* URLs */
		if (urlBase == null) {
			protocol = uri.getScheme();
			domain = uri.getAuthority();
			port = uri.getPort();
			urlBase = protocol + "://" + domain;
			urlBaseString = "PROTOCOL + \"://\" + DOMAIN";
			if (port != -1) {
				urlBase += ":" + port;
				urlBaseString += " + \":\" + PORT";
			}
		}

		String requestUrlBase = uri.getScheme() + "://" + uri.getAuthority();
		if (uri.getPort() != -1)
			requestUrlBase += ":" + uri.getPort();
		if (requestUrlBase.equals(urlBase))
			urls.put("url_" + id, uri.getPath());
		else
			urls.put("url_" + id, uri.toString());

		/* Headers */
		TreeMap<String, String> hm = new TreeMap<String, String>();
		for (Entry<String, String> entry : request.getHeaders()) {
			hm.put(entry.getKey(), entry.getValue());
		}
		headers.put("headers_" + id, hm);

		/* Params */
		QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
		e.getRequestParams().putAll((decoder.getParameters()));

		/* Content */
		if (request.getContent().capacity() > 0) {
			String content = new String(request.getContent().array());
			// We check if it's a form validation and so we extract post params
			if ("application/x-www-form-urlencoded".equals(request.getHeader("Content-Type"))) {
				decoder = new QueryStringDecoder("http://localhost/?" + content);
				e.getRequestParams().putAll(decoder.getParameters());
			} else {
				e.setWithBody(true);
				dumpRequestBody(id, content);
			}
		}
		listRequests.add(e);
	}

	private void dumpRequestBody(int idEvent, String content) {
		// Dump request body
		File dir = new File(configuration.getResultPath(), ResultType.FORMAT.format(startDate) + "_" + GATLING_REQUEST_BODIES_DIRECTORY_NAME);
		if (!dir.exists())
			dir.mkdir();

		FileWriter fw = null;
		try {
			fw = new FileWriter(new File(dir, "request_" + idEvent + ".txt"));
			fw.write(content);

		} catch (IOException ex) {
			System.err.println("Error, while dumping request body..." + ex.getStackTrace());

		} finally {
			if (fw != null) {
				try {
					fw.flush();
					fw.close();
				} catch (IOException ioe) {
					System.err.println(ioe);
				}
			}
		}
	}

	private void saveScenario() {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty("file.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.init();

		VelocityContext context = new VelocityContext();
		context.put("protocol", protocol);
		context.put("domain", domain);
		context.put("port", port);
		context.put("urlBase", urlBaseString);
		context.put("urls", urls);
		context.put("headers", headers);
		context.put("name", "Scenario name");
		context.put("reqs", listRequests);

		Template template = null;
		FileWriter fileWriter = null;
		for (ResultType resultType : configuration.getResultTypes()) {

			try {
				template = ve.getTemplate(resultType.getTemplate());
				fileWriter = new FileWriter(new File(configuration.getResultPath(), resultType.getScenarioFileName(startDate)));
				template.merge(context, fileWriter);
				fileWriter.flush();

			} catch (IOException e) {
				System.err.println("Error, while saving '" + resultType + "' scenario..." + e.getStackTrace());

			} finally {
				if (fileWriter != null) {
					try {
						fileWriter.close();
					} catch (IOException e) {
						System.err.println(e);
					}
				}
			}
		}
	}

	private String toRegexp(String pattern) {
		String regexpPattern = null;
		regexpPattern = pattern.replace(".", "\\.");
		regexpPattern = regexpPattern.replace("**", ".#?#?");
		regexpPattern = regexpPattern.replace("*", "[^/\\.]*");
		regexpPattern = regexpPattern.replace("#?#", "*");
		return regexpPattern;
	}
}