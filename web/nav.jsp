<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<nav class="navbar navbar-default">
    <div class="container-fluid">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1"
                aria-expanded="false">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Brand</a>
        </div>

        <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
            <ul class="nav navbar-nav">
                <c:url var="dashboardUrl" value="/dashboard"/>
                <li>
                    <a href="${dashboardUrl}">Content Explorer</a>
                </li>
                <c:url var="contentPickerUrl" value="/content-picker"/>
                <li>
                    <a href="${contentPickerUrl}">Content Picker</a>
                </li>
                <c:url var="contentUploaderUrl" value="/content-uploader"/>
                <li>
                    <a href="${contentUploaderUrl}">Content Uploader</a>
                </li>
                <c:url var="contentTreeUrl" value="/content-tree"/>
                <li>
                    <a href="${contentTreeUrl}">Content Tree</a>
                </li>
            </ul>
            <ul class="nav navbar-nav pull-right">
                <c:url var="logOutUrl" value="/logout?accessToken=${accessToken}"/>
                <li>
                    <a href="${logOutUrl}">Log Out</a>
                </li>
            </ul>
        </div>
    </div>
</nav>