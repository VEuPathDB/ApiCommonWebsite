<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="siteName" value="${applicationScope.wdkModel.name}" />
<div id="menubar">

<ul id="nav">
<li><a href="#">Searches &amp; Tools<img src="/assets/images/menu_divider3.png" width="20" height="11" /></a>
  <site:drop_down_QG />
</li>
<li>
	<a href="#">
		<div id="mysearch">My Saved Searches: 0</div>
	</a>
</li>
<li><a href="#"><img src="/assets/images/menu_divider4.png" width="20" height="11" />Data Sources<img src="/assets/images/menu_divider.png" width="24" height="11" /></a>
  <ul>
    <li><a href="#">Data Retrieval</a></li>
    <li><a href="#">Data and Methods</a></li>
    <li><a href="#">Web Tutorials</a></li>
    <li><a href="#">Glossary of Terms</a></li>
    <li><a href="#">Related Sites</a></li>
    <li><a href="#">Information and Help</a></li>
    <li><a href="#">Acknowledgements</a></li>
  </ul>
</li>
<li><a href="#">Downloads<img src="/assets/images/menu_divider.png" width="24" height="11" /></a></li>
<li><a href="#">About ${siteName}<img src="/assets/images/menu_divider.png" width="24" height="11" /></a>
  <ul>
    <li><a href="#">What is ${siteName}?</a></li>
    <li><a href="#">FAQs</a></li>
    <li><a href="#">Data Releases and Updates</a></li>
    <li><a href="#">Standard Operating Procedures</a></li>
    <li><a href="#">Publications</a></li>
    <li><a href="#">News</a></li>
    <li><a href="#">Data Access Policy</a></li>
    <li><a href="#">Data Contained in ${siteName}</a></li>
    <li><a href="#">Citing ${siteName}</a></li>
    <li><a href="#">Current Version Release Notes</a></li>
    <li><a href="#">Getting Support for ${siteName}</a></li>
    <li><a href="#">Site Statistics</a></li>
    <li><a href="#">Related Links</a></li>
    <li><a href="#">Contact Us</a></li>
  </ul>
</li>
<li><a href="#">Contact Us<img src="/assets/images/menu_divider.png" width="24" height="11" /></a></li>
<li><a href="#">Log In/Register<img src="/assets/images/menu_divider2.png" width="2" height="11" /></a></li>
</ul>
</div>
