# Proxy Spider

## Introduction

>This spider will establish a connection between a client browser and a web application or web page by acting as an intermediary proxy. To the client browser, there is no additional functionality with this proxy component. 

>By visiting a specified port (default port is 8080) of the spider, a basic web-page with a formatted table of domains will appear. Clicking on any one of these will return back an HTML document representing the sitemap of a website or web application.


## Installation

1. Pull the repository into Eclipse, Netbeans, or any method by which Maven can build the project.
2. Build and compile the project.
3. Run the project, optionally specifying a port number as an argument to `ProxySpider`.
  * Make sure the port isn't already bound to another application process


### Development
Want to contribute?

To fix a bug or enhance an existing module, follow these steps:

- Fork the repo
- Create a new branch (`git checkout -b improve-feature`)
- Make the appropriate changes in the files
- Add changes to reflect the changes made
- Commit your changes (`git commit -am 'Improve feature'`)
- Push to the branch (`git push origin improve-feature`)
- Create a Pull Request 

### Bug / Feature Request

If you find a bug, please open an issue by including your web request and the expected result. A packet capture would be immensely useful, as well.

If you'd like to request a new function, feel free to do so by also opening an issue. Please include examples and their expected results.


## Built with 

- [JavaDoc](http://www.oracle.com/technetwork/java/javase/tech/index-jsp-135444.html) - A tool for generating API documentation in HTML format from doc comments in source code.
- [Maven](https://developers.google.com/chart/interactive/docs/quick_start) - A build automation tool used primarily for Java projects.
- [JUnit](http://getbootstrap.com/) - A unit testing framework for the Java programming language. 


## To-do
- Better 400 and 404 error pages.
- Allow persistent connections over HTTP; pass HTML responses back for these.
- Add further compatibility with other HTTP encoding and compression algorithms.
- Get HTTPS certificate issues resolved for Microsoft Edge.

## License

Mozilla Public License 2.0
