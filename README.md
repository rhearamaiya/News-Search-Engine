# News-Search-Engine

This project is a news search engine, similar to [https://news.google.com](https://news.google.com), that uses [Common Crawl's news dataset](https://commoncrawl.org/2016/10/news-dataset-available/), which is stored in AWS S3 in the commoncrawl bucket at ```/crawl-data/CC-NEWS/```.

For Part 1 of this project, the goal was to download the latest WARC file (web archive) from Common Crawl and post its data to my Elastic Search database (on AWS). To do this, I wrote an ETL program (Extract, Transform, and Load), which extracted news articles from the online data set, transformed them into another format, and loaded them into my own database.

For Part 2 of the project, I built a stateless Java web application (backend) to use the data loaded from Part 1 (above) to search the database that can easily be run on any other machine, can handle many concurrent requests, and is configurable with environment variables. I wrote a REST API (via a Servlet <sup>1</sup>) to interact with the database so it could be searched. 

<sup>1</sup> Built a ".war" file, which is a special type of jar file that is meant to be run on a Java web application server, such as Tomcat.  Java web applications are written as subclasses of [javax.servlet.http.HttpServlet](https://tomcat.apache.org/tomcat-5.5-doc/servletapi/javax/servlet/http/HttpServlet.html), a built-in abstract class that provides a framework for handling HTTP requests

For Part 3 of the project, I made a basic React Javascript frontend that uses the REST API developed in Part 2 (built entirely in one big file, with JS and CSS embedded in the HTML). Features of the frontend: 
* Search Box
* Search results appear as you type.
* If more than ten search results are available, there is a control to see further pages of results on a new page (and a control to see previous results as well)
* Each search result includes:
     * The page title
     * The url (readable to the user), linked to visit page
     * A small snippet of text from the article, with search keywords highlighted.
* Controls to filter the search results by language and date
* Reset Search button
