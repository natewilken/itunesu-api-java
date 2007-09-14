/*
 * Copyright (c) 2007, Arizona State University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Arizona State University nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY ARIZONA STATE UNIVERSITY ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ARIZONA STATE UNIVERSITY BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.asu.itunesu;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.mail.MessagingException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

/**
 * The iTunesU Web Services API connection.
 *
 * @author <a href="mailto:ramen@asu.edu">Dave Benjamin</a>
 */
public class ITunesUConnection {
    private String siteUrl;
    private String debugSuffix;
    private String sharedSecret;
    private String[] credentials;

    private String identity;

    private boolean debug;

    private String requestValidationXsdPath;
    private String responseValidationXsdPath;
    private boolean dryRun;

    /**
     * @return the requestValidationXsdPath
     */
    public String getRequestValidationXsdPath() {
        return this.requestValidationXsdPath;
    }

    /**
     * @param requestValidationXsdPath Path to XSD file to validate requests, or null to skip request validation.
     */
    public void setRequestValidationXsdPath(String requestValidationXsdPath) {
        this.requestValidationXsdPath = requestValidationXsdPath;
    }

    /**
     * @return Path to XSD file to validate requests.
     */
    public String getResponseValidationXsdPath() {
        return this.responseValidationXsdPath;
    }

    /**
     * @param responseValidationXsdPath Path to XSD file to validate responses, or null to skip response validation.
     */
    public void setResponseValidationXsdPath(String responseValidationXsdPath) {
        this.responseValidationXsdPath = responseValidationXsdPath;
    }

    /**
     * @return the dryRun
     */
    public boolean getDryRun() {
        return this.dryRun;
    }

    /**
     * @param dryRun If true, request documents will not actually be sent to the iTunes U server.
     *               This can be used to perform validation on requests only.
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Constructor.
     *
     * @param siteUrl      The URL to your site in iTunes U. The last
     *                     component of that URL, after the last slash,
     *                     is a domain name that uniquely identifies your
     *                     site within iTunes U.
     * @param debugSuffix  A suffix you can append to your site URL
     *                     to obtain debugging information about the
     *                     transmission of credentials and identity
     *                     information from your institution's
     *                     authentication and authorization services
     *                     to iTunes U.
     * @param sharedSecret A secret key known only to you and Apple that
     *                     allows you to control who has access to your
     *                     site and what access they have to it.
     * @param credentials  The credential strings to assign
     *                     to users who should have the
     *                     permission to administer your
     *                     iTunes U site.
     */
    public ITunesUConnection(String siteUrl,
                             String debugSuffix,
                             String sharedSecret,
                             String[] credentials) {
        this.siteUrl = siteUrl;
        this.debugSuffix = debugSuffix;
        this.sharedSecret = sharedSecret;
        this.credentials = credentials;
        this.debug = false;
        this.requestValidationXsdPath = null;
        this.responseValidationXsdPath = null;
        this.dryRun = false;
    }

    /**
     * Sets the identity from user information.
     * Calling this method is optional. All parameters may be null.
     *
     * @param displayName The user's name.
     * @param emailAddress The user's email address.
     * @param username The user's username.
     * @param userIdentifier A unique identifier for the user.
     */
    public void setIdentity(String displayName,
                            String emailAddress,
                            String username,
                            String userIdentifier) throws ITunesUException {
        ITunesU iTunesU = new ITunesU();
        this.identity = iTunesU.getIdentityString(displayName,
                                                  emailAddress,
                                                  username,
                                                  userIdentifier);
    }

    /**
     * Sets the identity from a string.
     *
     * @param identity The identity as a string.
     */
    public void setIdentity(String identity) {
        this.identity = identity;
    }

    /**
     * Gets the value of the debug flag which determines whether or not to
     * use the debug prefix in requests. This flag is false by default.
     *
     * @return True if the debug prefix is enabled.
     */
    public boolean getDebug() {
        return this.debug;
    }

    /**
     * Sets the value of the debug flag which determines whether or not to
     * use the debug prefix in requests.
     *
     * @param debug True to enable the debug prefix, false otherwise.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Retrieves the entire site.
     *
     * @return A {@link Site} model object.
     */
    public Site getSite() throws ITunesUException {
        return Site.fromXml(this.showTree(null));
    }

    /**
     * Retrieves the entire site, loading a minimal amount of data.
     * Permissions and tracks are omitted. Only the name and handle
     * attributes are populated.
     *
     * @return A {@link Site} model object.
     */
    public Site getSiteMinimal() throws ITunesUException {
        return Site.fromXml(this.showTree(null, "minimal"));
    }

    /**
     * Retrieves a section by its handle.
     *
     * @param handle The handle of the section.
     * @return A {@link Section} model object.
     */
    public Section getSection(String handle) throws ITunesUException {
        return Section.fromXml(this.showTree(handle));
    }

    /**
     * Retrieves a division by its handle.
     *
     * @param handle The handle of the division.
     * @return A {@link Division} model object.
     */
    public Division getDivision(String handle) throws ITunesUException {
        return Division.fromXml(this.showTree(handle));
    }

    /**
     * Retrieves a course by its handle.
     *
     * @param handle The handle of the course.
     * @return A {@link Course} model object.
     */
    public Course getCourse(String handle) throws ITunesUException {
        return Course.fromXml(this.showTree(handle));
    }

    /**
     * Retrieves a group by its handle.
     *
     * @param handle The handle of the group.
     * @return A {@link Group} model object.
     */
    public Group getGroup(String handle) throws ITunesUException {
        return Group.fromXml(this.showTree(handle));
    }

    /**
     * Updates site information. Same as calling mergeSite() with
     * mergeByHandle and destructive set to false.
     *
     * @param siteHandle Handle for the site to update.
     * @param site Object containing site information.
     */
    public void mergeSite(String siteHandle, Site site)
        throws ITunesUException {

        this.mergeSite(siteHandle, site, false, false);
    }

    /**
     * Updates site information. Same as calling mergeSite() with
     * destructive set to false.
     *
     * @param siteHandle Handle for the site to update.
     * @param site Object containing site information.
     * @param mergeByHandle If true, merge sections by handle.
     *                      Otherwise, merge by name.
     */
    public void mergeSite(String siteHandle,
                          Site site,
                          boolean mergeByHandle)
        throws ITunesUException {

        this.mergeSite(siteHandle, site, mergeByHandle, false);
    }

    /**
     * Updates site information.
     *
     * @param siteHandle Handle for the site to update.
     * @param site Object containing site information.
     * @param mergeByHandle If true, merge sections by handle.
     *                      Otherwise, merge by name.
     * @param destructive If true, delete any unspecified
     *                    sections or permissions.
     */
    public void mergeSite(String siteHandle,
                          Site site,
                          boolean mergeByHandle,
                          boolean destructive)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        if (siteHandle != null) {
            arguments.put("SiteHandle", siteHandle);
        }

        arguments.put("MergeByHandle", mergeByHandle ? "true" : "false");
        arguments.put("Destructive", destructive ? "true" : "false");
        arguments.put("Site", site);

        ITunesUDocument doc = new ITunesUDocument("MergeSite", arguments);
        this.send(null, doc);
    }

    /**
     * Adds a division to a section.
     *
     * @param parentHandle Handle for the parent section.
     * @param division Object containing division information.
     */
    public String addDivision(String parentHandle,
                            Division division)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("ParentHandle", parentHandle);
        // todo: ParentPath?
        arguments.put("Division", division);

        ITunesUDocument doc = new ITunesUDocument("AddDivision", arguments);
        return this.send(null, doc).getAddedObjectHandle();
    }

    /**
     * Deletes a division from a section.
     *
     * @param divisionHandle Handle for the division to delete.
     */
    public void deleteDivision(String divisionHandle)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("DivisionHandle", divisionHandle);
        // todo: DivisionPath?

        ITunesUDocument doc = new ITunesUDocument("DeleteDivision", arguments);
        this.send(null, doc);
    }

    /**
     * Updates division information. Same as calling mergeDivision() with
     * mergeByHandle and destructive set to false.
     *
     * @param divisionHandle Handle for the division to update.
     * @param division Object containing division information.
     */
    public void mergeDivision(String divisionHandle, Division division)
        throws ITunesUException {

        this.mergeDivision(divisionHandle, division, false, false);
    }

    /**
     * Updates division information. Same as calling mergeDivision() with
     * destructive set to false.
     *
     * @param divisionHandle Handle for the division to update.
     * @param division Object containing division information.
     * @param mergeByHandle If true, merge sections by handle.
     *                      Otherwise, merge by name.
     */
    public void mergeDivision(String divisionHandle,
                              Division division,
                              boolean mergeByHandle)
        throws ITunesUException {

        this.mergeDivision(divisionHandle, division, mergeByHandle, false);
    }

    /**
     * Updates division information.
     *
     * @param divisionHandle Handle for the division to update.
     * @param division Object containing division information.
     * @param mergeByHandle If true, merge sections by handle.
     *                      Otherwise, merge by name.
     * @param destructive If true, delete any unspecified
     *                    sections or permissions.
     */
    public void mergeDivision(String divisionHandle,
                              Division division,
                              boolean mergeByHandle,
                              boolean destructive)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("DivisionHandle", divisionHandle);
        // todo: DivisionPath?
        arguments.put("MergeByHandle", mergeByHandle ? "true" : "false");
        arguments.put("Destructive", destructive ? "true" : "false");
        arguments.put("Division", division);

        ITunesUDocument doc = new ITunesUDocument("MergeDivision", arguments);
        this.send(null, doc);
    }

    /**
     * Adds a section to a site or division.
     *
     * @param parentHandle Handle for the parent site or division.
     * @param section Object containing section information.
     */
    public String addSection(String parentHandle,
                           Section section)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("ParentHandle", parentHandle);
        // todo: ParentPath
        arguments.put("Section", section);

        ITunesUDocument doc = new ITunesUDocument("AddSection", arguments);
        return this.send(null, doc).getAddedObjectHandle();
    }

    /**
     * Deletes a section from a site or division.
     *
     * @param sectionHandle Handle for the section to delete.
     */
    public void deleteSection(String sectionHandle)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("SectionHandle", sectionHandle);
        // todo: SectionPath?

        ITunesUDocument doc = new ITunesUDocument("DeleteSection", arguments);
        this.send(null, doc);
    }

    /**
     * Updates section information. Same as calling mergeSection() with
     * mergeByHandle and destructive set to false.
     *
     * @param sectionHandle Handle for the section to update.
     * @param section Object containing section information.
     */
    public void mergeSection(String sectionHandle, Section section)
        throws ITunesUException {

        this.mergeSection(sectionHandle, section, false, false);
    }

    /**
     * Updates section information. Same as calling mergeSection() with
     * destructive set to false.
     *
     * @param sectionHandle Handle for the section to update.
     * @param section Object containing section information.
     * @param mergeByHandle If true, merge items by handle.
     *                      Otherwise, merge by name.
     */
    public void mergeSection(String sectionHandle,
                             Section section,
                             boolean mergeByHandle)
        throws ITunesUException {

        this.mergeSection(sectionHandle, section, mergeByHandle, false);
    }

    /**
     * Updates section information.
     *
     * @param sectionHandle Handle for the section to update.
     * @param section Object containing section information.
     * @param mergeByHandle If true, merge items by handle.
     *                      Otherwise, merge by name.
     * @param destructive If true, delete any unspecified
     *                    items or permissions.
     */
    public void mergeSection(String sectionHandle,
                             Section section,
                             boolean mergeByHandle,
                             boolean destructive)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("SectionHandle", sectionHandle);
        // todo: SectionPath?
        arguments.put("Section", section);
        arguments.put("MergeByHandle", mergeByHandle ? "true" : "false");
        arguments.put("Destructive", destructive ? "true" : "false");

        ITunesUDocument doc = new ITunesUDocument("MergeSection", arguments);
        this.send(null, doc);
    }

    /**
     * Adds a course to a section.
     *
     * @param parentHandle Handle for the parent section.
     * @param templateHandle Handle for the course template, or null if none.
     * @param course Object containing course information.
     */
    public String addCourse(String parentHandle,
                          String templateHandle,
                          Course course)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("ParentHandle", parentHandle);

        // todo: ParentPath?

        if (templateHandle != null) {
            arguments.put("TemplateHandle", templateHandle);
        }

        arguments.put("Course", course);

        ITunesUDocument doc = new ITunesUDocument("AddCourse", arguments);
        return this.send(null, doc).getAddedObjectHandle();
    }

    /**
     * Deletes a course from a section.
     *
     * @param courseHandle Handle for the course to delete.
     */
    public void deleteCourse(String courseHandle)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("CourseHandle", courseHandle);
        // todo: CoursePath?

        ITunesUDocument doc = new ITunesUDocument("DeleteCourse", arguments);
        this.send(null, doc);
    }

    /**
     * Updates course information. Same as calling mergeCourse() with
     * mergeByHandle and destructive set to false.
     *
     * @param courseHandle Handle for the course to update.
     * @param course Object containing course information.
     */
    public void mergeCourse(String courseHandle, Course course)
        throws ITunesUException {

        this.mergeCourse(courseHandle, course, false, false);
    }

    /**
     * Updates course information. Same as calling mergeCourse() with
     * destructive set to false.
     *
     * @param courseHandle Handle for the course to update.
     * @param course Object containing course information.
     * @param mergeByHandle If true, merge groups by handle.
     *                      Otherwise, merge by name.
     */
    public void mergeCourse(String courseHandle,
                            Course course,
                            boolean mergeByHandle)
        throws ITunesUException {

        this.mergeCourse(courseHandle, course, mergeByHandle, false);
    }

    /**
     * Updates course information.
     *
     * @param courseHandle Handle for the course to update.
     * @param course Object containing course information.
     * @param mergeByHandle If true, merge groups by handle.
     *                      Otherwise, merge by name.
     * @param destructive If true, delete any unspecified
     *                    groups or permissions.
     */
    public void mergeCourse(String courseHandle,
                            Course course,
                            boolean mergeByHandle,
                            boolean destructive)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("CourseHandle", courseHandle);
        // todo: CoursePath?
        arguments.put("MergeByHandle", mergeByHandle ? "true" : "false");
        arguments.put("Destructive", destructive ? "true" : "false");
        arguments.put("Course", course);

        ITunesUDocument doc = new ITunesUDocument("MergeCourse", arguments);
        this.send(null, doc);
    }

    /**
     * Adds a group (also called a tab) to a course.
     *
     * @param parentHandle Handle for the parent course.
     * @param group Object containing group information.
     */
    public String addGroup(String parentHandle, Group group)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("ParentHandle", parentHandle);
        // todo: ParentPath?
        arguments.put("Group", group);

        ITunesUDocument doc = new ITunesUDocument("AddGroup", arguments);
        return this.send(null, doc).getAddedObjectHandle();
    }

    /**
     * Deletes a group from a course.
     *
     * @param groupHandle Handle for the group to delete.
     */
    public void deleteGroup(String groupHandle)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("GroupHandle", groupHandle);
        // todo: GroupPath?

        ITunesUDocument doc = new ITunesUDocument("DeleteGroup", arguments);
        this.send(null, doc);
    }

    /**
     * Updates group information. Same as calling mergeGroup() with
     * destructive set to false.
     *
     * @param groupHandle Handle for the group to update.
     * @param group Object containing group information.
     */
    public void mergeGroup(String groupHandle, Group group)
        throws ITunesUException {

        this.mergeGroup(groupHandle, group, false, false);
    }

    /**
     * Updates group information. Same as calling mergeGroup() with
     * destructive set to false.
     *
     * @param groupHandle Handle for the group to update.
     * @param group Object containing group information.
     * @param mergeByHandle If true, merge tracks by handle.
     *                      Otherwise, merge by name.
     */
    public void mergeGroup(String groupHandle,
                           Group group,
                           boolean mergeByHandle)
        throws ITunesUException {

        this.mergeGroup(groupHandle, group, mergeByHandle, false);
    }

    /**
     * Updates group information.
     *
     * @param groupHandle Handle for the group to update.
     * @param group Object containing group information
     * @param mergeByHandle If true, merge tracks by handle.
     *                      Otherwise, merge by name.
     * @param destructive If true, delete any unspecified
     *                    tracks or permissions.
     */
    public void mergeGroup(String groupHandle,
                           Group group,
                           boolean mergeByHandle,
                           boolean destructive)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("GroupHandle", groupHandle);
        // todo: GroupPath?
        arguments.put("MergeByHandle", mergeByHandle ? "true" : "false");
        arguments.put("Destructive", destructive ? "true" : "false");
        arguments.put("Group", group);

        ITunesUDocument doc = new ITunesUDocument("MergeGroup", arguments);
        this.send(null, doc);
    }

    /**
     * Adds a track to a group.
     *
     * @param parentHandle Handle for the parent group.
     * @param track Object containing track information.
     */
    public String addTrack(String parentHandle,
                         Track track)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("ParentHandle", parentHandle);
        // todo: ParentPath?
        arguments.put("Track", track);

        ITunesUDocument doc = new ITunesUDocument("AddTrack", arguments);
        return this.send(null, doc).getAddedObjectHandle();
    }

    /**
     * Deletes a track from a group.
     *
     * @param trackHandle Handle for the track to delete.
     */
    public void deleteTrack(String trackHandle)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("TrackHandle", trackHandle);

        ITunesUDocument doc = new ITunesUDocument("DeleteTrack", arguments);
        this.send(null, doc);
    }

    /**
     * Updates track information.
     *
     * @param trackHandle Handle for the track to update.
     * @param track Object containing track information.
     */
    public void mergeTrack(String trackHandle,
                           Track track)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("TrackHandle", trackHandle);
        arguments.put("Track", track);

        ITunesUDocument doc = new ITunesUDocument("MergeTrack", arguments);
        this.send(null, doc);
    }

    /**
     * Adds a permission to a section, course, or group.
     *
     * @param parentHandle Handle for the parent section, course, or group.
     * @param permission Object containing permission information.
     */
    public String addPermission(String parentHandle,
                              Permission permission)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("ParentHandle", parentHandle);
        // todo: ParentPath?
        arguments.put("Permission", permission);

        ITunesUDocument doc = new ITunesUDocument("AddPermission", arguments);
        return this.send(null, doc).getAddedObjectHandle();
    }

    /**
     * Deletes a permission from a section, course, or group.
     *
     * @param parentHandle Handle for the parent section, course, or group.
     * @param credential Credential of permission to delete.
     */
    public void deletePermission(String parentHandle,
                                 String credential)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("ParentHandle", parentHandle);
        // todo: ParentPath?
        arguments.put("Credential", credential);

        ITunesUDocument doc =
            new ITunesUDocument("DeletePermission", arguments);
        this.send(null, doc);
    }

    /**
     * Updates a permission.
     *
     * @param parentHandle Handle for the parent section, course, or group.
     * @param permission Object containing permission information.
     */
    public void mergePermission(String parentHandle,
                                Permission permission)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("ParentHandle", parentHandle);
        // todo: ParentPath?
        arguments.put("Permission", permission);

        ITunesUDocument doc = new ITunesUDocument("MergePermission", arguments);
        this.send(null, doc);
    }

    /**
     * Adds a credential.
     *
     * @param credential The credential to add.
     */
    public void addCredential(String credential) throws ITunesUException {
        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("Credential", credential);

        ITunesUDocument doc = new ITunesUDocument("AddCredential", arguments);
        this.send(null, doc);
    }

    /**
     * Deletes a credential.
     *
     * @param credential The credential to delete.
     */
    public void deleteCredential(String credential) throws ITunesUException {
        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("Credential", credential);

        ITunesUDocument doc = new ITunesUDocument("DeleteCredential", arguments);
        this.send(null, doc);
    }

    /**
     * Reads XML for an element by its handle.
     *
     * @param handle Handle of the parent element, or null for the whole site.
     * @return An XML string.
     */
    public String showTree(String handle) throws ITunesUException {
        String prefix = this.getPrefix();
        String url = prefix + "/API/ShowTree/" + this.getDestination(handle);

        ITunesU iTunesU = new ITunesU();

        try {
            return iTunesU.invokeAction(url, this.generateToken());
        } catch (AssertionError e) {
            throw new ITunesUException(e);
        }
    }

    /**
     * Reads XML for an element by its handle, specifying a key group.
     *
     * @param handle Handle of the parent element, or null for the whole site.
     * @param keyGroup Must be one of: minimal, most, maximal
     * @return An XML string.
     */
    public String showTree(String handle, String keyGroup)
        throws ITunesUException {

        Map<String, Object> arguments = new LinkedHashMap<String, Object>();

        arguments.put("Handle", handle != null ? handle : "");
        arguments.put("KeyGroup", keyGroup);

        ITunesUDocument doc = new ITunesUDocument("ShowTree", arguments);

        String requestXml;
        try {
            requestXml = doc.toXml();;
        } catch (ParserConfigurationException e) {
            throw new ITunesUException(e);
        } catch (TransformerException e) {
            throw new ITunesUException(e);
        }

        validateRequestXml(requestXml);
        if (this.dryRun) {
            return null;
        }
        String responseXml = this.execute(handle, requestXml);
        validateResponseXml(responseXml);

        ITunesUResponse response = ITunesUResponse.fromXml(responseXml);
        if (response.getError() != null) {
        	throw new ITunesUException(response.getError());
        } else {
        	return response.getResultXml();
        }
    }

    /**
     * Reads RSS feed XML for an element by its handle.
     *
     * @param handle Handle of the element.
     * @return An XML string.
     */
    public String showFeed(String handle) throws ITunesUException {
        String prefix = this.getPrefix();
        String url = prefix + "/Feed/" + this.getDestination(handle);

        ITunesU iTunesU = new ITunesU();

        try {
            return iTunesU.invokeAction(url, this.generateToken());
        } catch (AssertionError e) {
            throw new ITunesUException(e);
        }
    }

    public String getDailyReportLogs(String startDate, String endDate) throws ITunesUException {
        ITunesU iTunesU = new ITunesU();

        String url = (this.getPrefix()
                      + "/API/GetDailyReportLogs/"
                      + this.getDestination(null)
                      + "?StartDate=" + startDate);

        if (endDate != null) {
            url += "&EndDate=" + endDate;
        }

        try {
            return iTunesU.invokeAction(url, this.generateToken());
        } catch (AssertionError e) {
            throw new ITunesUException(e);
        }
    }

    /**
     * Generates and returns a new iTunesU upload URL.
     *
     * @param handle Handle for the destination.
     * @param forXml True for uploading XML, false for uploading content.
     * @return The URL as a string.
     */
    public String getUploadUrl(String handle, boolean forXml)
        throws ITunesUException {

        ITunesU iTunesU = new ITunesU();

        String url = (this.getPrefix()
                      + "/API/GetUploadURL/"
                      + this.getDestination(handle));

        if (forXml) {
            url += "?type=XMLControlFile";
        }

        try {
            return iTunesU.invokeAction(url, this.generateToken());
        } catch (AssertionError e) {
            throw new ITunesUException(e);
        }
    }

    private ITunesUResponse send(String handle, ITunesUDocument doc)
        throws ITunesUException {

        String requestXml;
        try {
            requestXml = doc.toXml();;
        } catch (ParserConfigurationException e) {
            throw new ITunesUException(e);
        } catch (TransformerException e) {
            throw new ITunesUException(e);
        }

        validateRequestXml(requestXml);
        if (this.dryRun) {
            return null;
        }
        String responseXml = this.execute(handle, requestXml);
        validateResponseXml(responseXml);
        
        ITunesUResponse response = ITunesUResponse.fromXml(responseXml);
        if (response.getError() != null) {
        	throw new ITunesUException(response.getError());
        } else {
        	return response;
        }
    }

    private void validateRequestXml(String xml) throws ITunesUException {
        if (this.requestValidationXsdPath != null) {
        	
        	// debug:
        	System.out.println("Validating the following XML against " + this.requestValidationXsdPath + "\n" + xml + "\n");

        	SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            File schemaLocation = new File(this.requestValidationXsdPath);
            try {
                Schema schema = factory.newSchema(schemaLocation);
                Validator validator = schema.newValidator();
                Source source = new StreamSource(new StringReader(xml));
                validator.validate(source);
            } catch (SAXException e) {
                throw new ITunesUException(e);
            } catch (IOException e) {
                throw new ITunesUException(e);
            }
        }
    }

    private void validateResponseXml(String xml) throws ITunesUException {
        if (this.responseValidationXsdPath != null) {

        	// debug:
        	System.out.println("Validating the following XML against " + this.responseValidationXsdPath + "\n" + xml + "\n");
            
        	SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            File schemaLocation = new File(this.responseValidationXsdPath);
            try {
                Schema schema = factory.newSchema(schemaLocation);
                Validator validator = schema.newValidator();
                Source source = new StreamSource(new StringReader(xml));
                validator.validate(source);
            } catch (SAXException e) {
                throw new ITunesUException(e);
            } catch (IOException e) {
                throw new ITunesUException(e);
            }
        }
    }

    private String execute(String handle, String xml)
        throws ITunesUException {

        ITunesUFilePOST iTunesUFilePOST = new ITunesUFilePOST();
        try {
            return iTunesUFilePOST.invokeAction(this.getUploadUrl(handle, true),
                                                "file",
                                                "file.xml",
                                                xml,
                                                "text/xml");
        } catch (MessagingException e) {
            throw new ITunesUException(e);
        } catch (AssertionError e) {
            throw new ITunesUException(e);
        }
    }

    private String generateToken() throws ITunesUException {
        ITunesU iTunesU = new ITunesU();

        String credentials = iTunesU.getCredentialsString(this.credentials);
        String identity = this.identity == null ? "" : this.identity;
        Date now = new Date();
        byte[] key;

        try {
            key = sharedSecret.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new ITunesUException(e);
        }

        String token = iTunesU.getAuthorizationToken(credentials, identity, now, key);
        return token;
    }

    private String getDestination(String handle) {
        String destination = this.getSiteDomain();

        if (handle != null) {
            destination += "." + handle;
        }

        if (this.debug) {
            destination += this.debugSuffix;
        }

        return destination;
    }

    private String getPrefix() {
        return this.siteUrl.substring(0, this.siteUrl.indexOf(".woa/") + 4);
    }

    private String getSiteDomain() {
        return this.siteUrl.substring(this.siteUrl.lastIndexOf('/') + 1);
    }
}
