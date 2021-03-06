// Copyright 2017 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package adwords.axis.v201806.remarketing;

import static com.google.api.ads.common.lib.utils.Builder.DEFAULT_CONFIGURATION_FILENAME;

import com.google.api.ads.adwords.axis.factory.AdWordsServices;
import com.google.api.ads.adwords.axis.utils.v201806.SelectorBuilder;
import com.google.api.ads.adwords.axis.v201806.cm.AdWordsConversionTracker;
import com.google.api.ads.adwords.axis.v201806.cm.ApiError;
import com.google.api.ads.adwords.axis.v201806.cm.ApiException;
import com.google.api.ads.adwords.axis.v201806.cm.ConversionTracker;
import com.google.api.ads.adwords.axis.v201806.cm.ConversionTrackerPage;
import com.google.api.ads.adwords.axis.v201806.cm.ConversionTrackerServiceInterface;
import com.google.api.ads.adwords.axis.v201806.cm.Operator;
import com.google.api.ads.adwords.axis.v201806.cm.Selector;
import com.google.api.ads.adwords.axis.v201806.rm.AdwordsUserListServiceInterface;
import com.google.api.ads.adwords.axis.v201806.rm.BasicUserList;
import com.google.api.ads.adwords.axis.v201806.rm.UserList;
import com.google.api.ads.adwords.axis.v201806.rm.UserListConversionType;
import com.google.api.ads.adwords.axis.v201806.rm.UserListMembershipStatus;
import com.google.api.ads.adwords.axis.v201806.rm.UserListOperation;
import com.google.api.ads.adwords.axis.v201806.rm.UserListReturnValue;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.factory.AdWordsServicesInterface;
import com.google.api.ads.adwords.lib.selectorfields.v201806.cm.AdwordsUserListField;
import com.google.api.ads.common.lib.auth.OfflineCredentials;
import com.google.api.ads.common.lib.auth.OfflineCredentials.Api;
import com.google.api.ads.common.lib.conf.ConfigurationLoadException;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.api.client.auth.oauth2.Credential;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This example adds a remarketing user list (a.k.a. audience).
 *
 * <p>Credentials and properties in {@code fromFile()} are pulled from the
 * "ads.properties" file. See README for more info.
 */
public class AddAudience {

  public static void main(String[] args) {
    AdWordsSession session;
    try {
      // Generate a refreshable OAuth2 credential.
      Credential oAuth2Credential =
          new OfflineCredentials.Builder()
              .forApi(Api.ADWORDS)
              .fromFile()
              .build()
              .generateCredential();

      // Construct an AdWordsSession.
      session =
          new AdWordsSession.Builder().fromFile().withOAuth2Credential(oAuth2Credential).build();
    } catch (ConfigurationLoadException cle) {
      System.err.printf(
          "Failed to load configuration from the %s file. Exception: %s%n",
          DEFAULT_CONFIGURATION_FILENAME, cle);
      return;
    } catch (ValidationException ve) {
      System.err.printf(
          "Invalid configuration in the %s file. Exception: %s%n",
          DEFAULT_CONFIGURATION_FILENAME, ve);
      return;
    } catch (OAuthException oe) {
      System.err.printf(
          "Failed to create OAuth credentials. Check OAuth settings in the %s file. "
              + "Exception: %s%n",
          DEFAULT_CONFIGURATION_FILENAME, oe);
      return;
    }

    AdWordsServicesInterface adWordsServices = AdWordsServices.getInstance();

    try {
      runExample(adWordsServices, session);
    } catch (ApiException apiException) {
      // ApiException is the base class for most exceptions thrown by an API request. Instances
      // of this exception have a message and a collection of ApiErrors that indicate the
      // type and underlying cause of the exception. Every exception object in the adwords.axis
      // packages will return a meaningful value from toString
      //
      // ApiException extends RemoteException, so this catch block must appear before the
      // catch block for RemoteException.
      System.err.println("Request failed due to ApiException. Underlying ApiErrors:");
      if (apiException.getErrors() != null) {
        int i = 0;
        for (ApiError apiError : apiException.getErrors()) {
          System.err.printf("  Error %d: %s%n", i++, apiError);
        }
      }
    } catch (RemoteException re) {
      System.err.printf(
          "Request failed unexpectedly due to RemoteException: %s%n", re);
    }
  }

  /**
   * Runs the example.
   *
   * @param adWordsServices the services factory.
   * @param session the session.
   * @throws ApiException if the API request failed with one or more service errors.
   * @throws RemoteException if the API request failed due to other errors.
   */
  public static void runExample(
      AdWordsServicesInterface adWordsServices, AdWordsSession session) throws RemoteException {
    // Get the UserListService.
    AdwordsUserListServiceInterface userListService =
        adWordsServices.get(session, AdwordsUserListServiceInterface.class);

    // Get the ConversionTrackerService.
    ConversionTrackerServiceInterface conversionTrackerService =
        adWordsServices.get(session, ConversionTrackerServiceInterface.class);

    // Create conversion type (tag).
    UserListConversionType conversionType = new UserListConversionType();
    conversionType.setName("Mars cruise customers #" + System.currentTimeMillis());

    // Create remarketing user list.
    BasicUserList userList = new BasicUserList();
    userList.setName("Mars cruise customers #" + System.currentTimeMillis());
    userList.setDescription("A list of mars cruise customers in the last year");
    userList.setMembershipLifeSpan(365L);
    userList.setConversionTypes(new UserListConversionType[] {conversionType});

    // You can optionally provide these field(s).
    userList.setStatus(UserListMembershipStatus.OPEN);

    // Create operations.
    UserListOperation operation = new UserListOperation();
    operation.setOperand(userList);
    operation.setOperator(Operator.ADD);

    UserListOperation[] operations = new UserListOperation[] {operation};

    // Add user list.
    UserListReturnValue result = userListService.mutate(operations);

    // Display results.
    // Capture the ID(s) of the conversion.
    List<String> conversionIds = new ArrayList<>();
    for (UserList userListResult : result.getValue()) {
      if (userListResult instanceof BasicUserList) {
        BasicUserList remarketingUserList = (BasicUserList) userListResult;
        for (UserListConversionType userListConversionType :
            remarketingUserList.getConversionTypes()) {
          conversionIds.add(userListConversionType.getId().toString());
        }
      }
    }

    // Create predicate and selector.
    Selector selector = new SelectorBuilder()
        .fields("Id", "GoogleGlobalSiteTag", "GoogleEventSnippet")
        .in(AdwordsUserListField.Id, conversionIds.toArray(new String[0]))
        .build();

    // Get all conversion trackers.
    Map<Long, AdWordsConversionTracker> conversionTrackers =
        new HashMap<Long, AdWordsConversionTracker>();
    ConversionTrackerPage page = conversionTrackerService.get(selector);
    if (page != null && page.getEntries() != null) {
      conversionTrackers =
          Arrays.stream(page.getEntries())
              .collect(
                  Collectors.toMap(
                      conversionTracker -> conversionTracker.getId(),
                      conversionTracker -> (AdWordsConversionTracker) conversionTracker));
    }

    // Display user lists.
    for (UserList userListResult : result.getValue()) {
      System.out.printf("User list with name '%s' and ID %d was added.%n",
          userListResult.getName(), userListResult.getId());

      // Display user list associated conversion code snippets.
      if (userListResult instanceof BasicUserList) {
        BasicUserList remarketingUserList = (BasicUserList) userListResult;
        for (UserListConversionType userListConversionType : remarketingUserList
            .getConversionTypes()) {
          ConversionTracker conversionTracker =
              conversionTrackers.get(userListConversionType.getId());
          System.out.printf(
              "Google global site tag:%n%s%n%n", conversionTracker.getGoogleGlobalSiteTag());
          System.out.printf(
              "Google event snippet:%n%s%n%n", conversionTracker.getGoogleEventSnippet());
        }
      }
    }
  }
}
