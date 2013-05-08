package principal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resources;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;


public class HoteltoRDF {

	/*private static final String DATA_URL = "http://data.paysdelaloire.fr/api/publication/" +
			"22440002800011_CG44_TOU_04815/hotels_STBL/content/" +
			"?format=xml&" +
			"filter={\"VIL\":{\"$eq\":\"NANTES\"}}";*/
	
	private static final String DATA_URL =
		    "http://data.nantes.fr/api/publication/"
				    + "22440002800011_CG44_TOU_04815/"
				    + "hotels_STBL/content"
				    + "?format=xml";

	private static final String ONTO_PREFIX = "ex";
	private static final String ONTO_URL = "http://example.org/";

	private static final String OUTPUT_XML = "hotel.xml";
	private static final String MAPPING_FILE = "/home/seb/Documents/TER/V3/gun2012/code/expfiles/berlinData/DATASET/n3dir/hotel/mapping2.properties";

	// Only for convenience...
	private static HashMap<String, String> mapping = 
			new HashMap<String, String>();
	
	private String viewSparql_;
	private String outputFilePath_;
	private boolean StaticFile;


	public HoteltoRDF(){
		System.setProperty("http.proxyHost", "cache.etu.univ-nantes.fr");
		System.setProperty("http.proxyPort", "3128");
		outputFilePath_ = "hotel";
		StaticFile = false;
	}

	public HoteltoRDF(String script) {
		// TODO Auto-generated constructor stub
		System.setProperty("http.proxyHost", "cache.etu.univ-nantes.fr");
		System.setProperty("http.proxyPort", "3128");
		viewSparql_ = script;
	}

	public Document getXml() throws IOException, JDOMException {
		URL url = new URL(DATA_URL);
		URLConnection connection = url.openConnection();

		InputStreamReader stream = null;
		if(!StaticFile){
			stream =	
				new InputStreamReader(connection.getInputStream());	}
		else{
			stream =
				new InputStreamReader(new FileInputStream(new File("hotelXML.xml")));}
		
		//writeXml();
		return new SAXBuilder().build(stream);
	}

	public void writeXml() throws JDOMException, IOException {
		XMLOutputter sortie =
				new XMLOutputter(Format.getPrettyFormat());
		sortie.output(this.getXml(),
				new FileOutputStream(new File(OUTPUT_XML)));
	}

	public void loadMapping() throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		prop.load(new FileReader(MAPPING_FILE));

		for(Object key : prop.keySet()) {
			mapping.put(key.toString(), prop.getProperty(key.toString()));
		}
		//System.out.println(mapping.toString());
	}

	public void toRdf() throws IOException, JDOMException {
		//this.loadMapping();
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix(ONTO_PREFIX, ONTO_URL);

		Document xml = this.getXml();
		Element root = xml.getRootElement();

		List<Element> listeActi = root.getChild("data").getChildren("element");
		int id = 0;
		// Every <element> matches with an activity
		for(Element activity : listeActi) {
			Resource res = null; 
					//model.createResource(ONTO_URL + id);

			// Every java property matches with an RDF property
			for(String key : mapping.keySet()) {
				//System.out.println(key);
				Element current = activity;

				String value = mapping.get(key);

				// Several elements (paths) which fit an RDF property
				// are separated by a coma
				String[] paths = value.split(",");

				for(int p = 0 ; p < paths.length ; p++) {
					String path = paths[p];
					String[] elements = path.split("\\.");//geo.name

					for(int e = 0 ; e < elements.length && current != null ; e++) {
						current = current.getChild(elements[e]);
					}

					if(current != null) {
						String text = current.getValue().replace("\"", "");

						if(text.equals("null") == false) {
							/*if(key.equals("hasName")) {
								String id = text.replace(' ', '_')
										.replace('\'', '_');
*/
								res = model.createResource(ONTO_URL + id);
							//}

							res.addProperty(
									model.createProperty(ONTO_URL, key),
									text);
						}
						//}
					}
				}
			}//fi
				id++;

		}
		//execution de la requete!
		
		String queryString = "";
		InputStream fp = new FileInputStream(viewSparql_);
		InputStreamReader ipsr=new InputStreamReader(fp);
		BufferedReader br=new BufferedReader(ipsr);
		String ligne;
		while ((ligne=br.readLine())!=null){
			System.out.println(ligne);
			queryString+=ligne+"\n";
		}
		br.close();
		
		OutputStream out = new FileOutputStream("/home/seb/Documents/TER/V3/gun2012/code/expfiles/berlinData/DATASET/n3dir/vue.n3");
		
		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
		
		//Model newModel = ModelFactory.createDefaultModel();
		
		try {
			   ResultSet results = qexec.execSelect() ;
			   /*for(;results.hasNext();){
				   QuerySolution qs = results.nextSolution();
				   Resource rs = qs.getResource("nom");				   
			   }*/
			   //ResultSetFormatter.output(new FileOutputStream("/home/seb/Documents/TER/V3/gun2012/code/expfiles/berlinData/DATASET/n3dir/vue.n3"), results, ResultsFormat.FMT_RDF_N3);
			   ResultSetFormatter.out(new FileOutputStream("/home/seb/Documents/TER/V3/gun2012/code/expfiles/berlinData/DATASET/n3dir/vue.n3"), results, query);
			   //Model newModel = ResultSetFormatter.toModel(results);
			   //newModel.write(new FileOutputStream("/home/seb/Documents/TER/V3/gun2012/code/expfiles/berlinData/DATASET/n3dir/vue.n3"),"N-TRIPLE");
			   
		} finally { qexec.close() ; }
		
		
		model.write(new FileOutputStream(outputFilePath_ +".n3"),"N-TRIPLE");
	}
}
