# iTunesU Web Services API for Java #

## Introduction ##

The iTunesU Web Services API for Java is an interface to the iTunesU Web
Services API written in the Java programming language. This API allows
software developers to write applications that integrate with iTunesU to
automate the creation and management of courses and multimedia documents for
the iTunesU system.

The iTunesU Web Services API provides services for the management of
iTunesU courses using an XML interface. This Java API provides the
necessary tools to use these XML-based services using native Java
classes. In doing so, it alleviates the need for application developers
to be concerned with XML manipulation or networking details.

## Getting Started ##

Use of this API requires Java 5 or newer.

### Downloading Libraries ###

Download the itunesu-api-java-(version).jar file from the
[Downloads](http://code.google.com/p/itunesu-api-java/downloads/list) section
and add its path to your CLASSPATH.

### Importing Classes ###

All of the classes are in the edu.asu.itunesu package, which you can import
with the following statement:

```
    import edu.asu.itunesu.*;
```

### Creating Connections ###

The ITunesUConnection class is the main interface to this API. You will need a
few pieces of information, which you should have received when you registered
as an iTunesU API user:

```
    String siteURL = "https://deimos.apple.com/WebObjects/Core.woa/Browse/example.edu";
    String debugSuffix = "/abc123";
    String sharedSecret = "STRINGOFTHIRTYTWOLETTERSORDIGITS";
    String[] credentials = {"Administrator@urn:mace:itunesu.com:sites:example.edu"};
```

With these variables defined, you can create a connection:

```
    ITunesUConnection conn =
        new ITunesUConnection(siteURL, debugSuffix, sharedSecret, credentials);
```

Optionally, you can set the identity:

```
    conn.setIdentity(displayName, emailAddress, username, userIdentifier);
```

Or, using a string of your construction:

```
    conn.setIdentity(identity);
```

### Retrieving Information ###

You can start by retrieving the entire Site:

```
    Site site = conn.getSite();
```

A Site contains Sections:

```
    List sections = site.getSections();
```

Each Section contains a mix of Divisions and Courses, which share a common
interface called SectionItem. You can use the instanceof operator to determine
which of the two you are dealing with:

```
    for (Iterator i = sections.iterator(); i.hasNext();) {
        Section section = (Section) i.next();
        System.out.println("Found a Section: " + section);

        for (Iterator j = section.getSectionItems().iterator(); j.hasNext();) {
            SectionItem item = (SectionItem) j.next();

            if (item instanceof Division) {
                Division division = (Division) item;
                System.out.println("Found a Division: " + division);
            } else if (item instanceof Course) {
                Course course = (Course) item;
                System.out.println("Found a Course: " + course);
            }
        }

        System.out.println();
    }
```

Divisions contain more Sections, which in turn can contain Divisions and
Courses, forming a tree.

If you know what you're looking for, you can grab a course out of
the tree by indexes:

```
    Section section = (Section) sections.get(6);
    Division division = (Division) section.getSectionItems().get(0);
    Section subSection = (Section) division.getSections().get(0);
    Course course = (Course) subSection.getSectionItems().get(0);
```

Each Course contains Groups, which appear in iTunesU as tabs:

```
    for (Iterator i = course.getGroups().iterator(); i.hasNext();) {
        Group group = (Group) i.next();
        System.out.println("Found a Group: " + group);
    }
```

Each Group contains Tracks, which represent the individual media objects:

```
    Group group = (Group) course.getGroups().get(0);
    for (Iterator i = group.getTracks().iterator(); i.hasNext();) {
        Track track = (Track) i.next();
        System.out.println("Found a Track: " + track);
    }
```

### Updating Information ###

You can update a Course, Group, or Track by using the merge methods. To use
these methods, construct a new object containing only the data you want
to update. For instance, to rename a Group, do the following:

```
    Group groupUpdates = new Group();
    groupUpdates.setName("Test Tab 1");
    conn.mergeGroup(group.getHandle(), groupUpdates);
```

### Reloading Objects ###

After performing a merge operation, the data has been updated on Apple's
servers, but the Group object still contains the old data. To refresh the
Group object:

```
    group = conn.getGroup(group.getHandle());
    System.out.println(group.getName()); // Should print "Test Tab 1".
```

There are corresponding getCourse(), getTrack() and getSite() methods. There
is also a getSiteMinimal() method, which retrieves only the Name and Handle
attributes of each object. It is a good idea to use getSiteMinimal() if you
are dealing with a large tree, as it will load much faster. You can then use
the other get-methods to load details for the specific objects you are
interested in.

### Creating New Objects ###

The process of adding new objects to the tree is similar to the process of
updating existing ones: create an object, set some properties, and call the
appropriate method on the connection:

```
    Course course = new Course();
    course.setName("Test Course");
    course.setShortName("Test");
    conn.addCourse(section.getHandle(), courseTemplate.getHandle(), course);
```

The handle of a newly-created object can be retrieved from the ITunesUResponse
object:

```
    ITunesUResponse resp = conn.addCourse(...);
    System.out.println("New course handle: " + resp.getAddedObjectHandle());
```

### Copying and Moving Objects ###

Objects can be copied by using them as templates in the add methods. For instance, to copy a course, use addCourse and pass the source course's handle as the course template handle:

```
    Site site = conn.getSite();
    Course srcCourse = ...; // find the source course in the site tree
    Section destSection = ...; // find the destination section
    Course destCourse = new Course();
    // if you want to make any changes as you copy, call setters on destCourse here...
    ITunesUResponse resp = conn.addCourse(destSection.getHandle(), srcCourse.getHandle(), destCourse);
```

To "move", delete the source object after you copy:

```
    conn.deleteCourse(srcCourse.getHandle());
```

## Additional Documentation and Resources ##

  * [Latest iTunesU API JavaDocs](http://itunesu-api-java.googlecode.com/svn/trunk/doc/index.html)

  * [Older iTunesU API JavaDocs (1.4.1 Release)](http://itunesu-api-java.googlecode.com/svn/tags/1.4.1/doc/index.html)

  * [iTunesU Administrator's Guide](http://images.apple.com/support/itunes_u/docs/iTunesU_Admin_Guide_1S_acc.pdf)

  * [Apple's iTunesU Support site](http://www.apple.com/support/itunes_u/)

  * [Apple's iTunesU Site Administrators forum](http://discussions.apple.com/forum.jspa?forumID=1175)

## Troubleshooting ##

### Server returned HTTP response code: 403 for URL: ... ###

Check your system clock. It needs to be synchronized with a time server or the
time stamps on the authentication tokens will most likely be off. On a Mac,
this tends to work out of the box. On a Linux server, try "ntpdate" and/or
"ntpd".

### java.security.AccessControlException: access denied (java.net.SocketPermission deimos.apple.com resolve) ###

If you are trying to use the API from a JSP environment with a restrictive
security policy, such as Blackboard, you may need to adjust your policy
settings. According to _mcelve2_ on the Site Administrators forum:

```
I talked to some of my colleagues and they mentioned setting the socket
permissions in the bb.manifest.xml file.

I added:

<permission type="socket" name="*" actions="accept,connect"/>
<permission type="java.util.PropertyPermission" name="*" actions="read,write"/>

...between the open and close <permission> tags. This took care of the error
and I was able to retrieve the itunes site name successfully.
```

## Credits ##

The iTunesU Web Services API for Java was written by
[Dave Benjamin](mailto:ramen@asu.edu) for
[Arizona State University](http://www.asu.edu/).
Portions are based on
[sample code provided by Apple](http://images.apple.com/support/itunes_u/docs/iTunes_U_Code_Samples.zip),
(c) 2006 Apple Computer, Inc., made available for redistribution under the
iTunes U Sample Code License. The remainder of the code is (c) 2007-2008,
Arizona State University, and made freely available under the BSD License.