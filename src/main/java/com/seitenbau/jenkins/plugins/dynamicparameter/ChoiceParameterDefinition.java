/*
 * Copyright 2012 Seitenbau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.seitenbau.jenkins.plugins.dynamicparameter;

import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import net.sf.json.JSONObject;

import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/** Choice parameter, with dynamically generated list of values. */
public class ChoiceParameterDefinition extends ParameterDefinitionBase
{
  /** Serial version UID. */
  private static final long serialVersionUID = 5454277528808586236L;

  /**
   * Constructor.
   * @param name parameter name
   * @param script script, which generates the parameter value
   * @param description parameter description
   * @param uuid identifier (optional)
   * @param remote execute the script on a remote node
   */
  @DataBoundConstructor
  public ChoiceParameterDefinition(final String name, final String script,
      final String description, final String uuid, final boolean remote)
  {
    super(name, script, description, uuid, remote);
  }

  /** @return the possible choices, generated by the script */
  @SuppressWarnings("unchecked")
  public final List<Object> getChoices()
  {
    final Object value = getValue();

    if (value == null)
    {
      LOGGER.log(Level.WARNING, "Script for parameter '" + getName()
          + "' returned null!");
      return Collections.EMPTY_LIST;
    }

    if (!(value instanceof List))
    {
      LOGGER.log(Level.WARNING, "Script for parameter '" + getName()
          + "' did not return an instance of java.util.List!");
      return Collections.EMPTY_LIST;
    }

    return (List<Object>) value;
  }

  @Override
  public final ParameterValue createValue(final StaplerRequest req,
      final JSONObject jo)
  {
    final StringParameterValue v = req.bindJSON(StringParameterValue.class, jo);
    v.setDescription(getDescription());
    return checkValue(v);
  }

  @Override
  public final ParameterValue createValue(final StaplerRequest req)
  {
    final String[] values = req.getParameterValues(getName());

    if (values == null)
    {
      return getDefaultParameterValue();
    }
    else if (values.length == 1)
    {
      return checkValue(new StringParameterValue(getName(), values[0],
          getDescription()));
    }
    else
    {
      throw new IllegalArgumentException(
          "Illegal number of parameter values for " + getName() + ": "
              + values.length);
    }
  }

  /**
   * Check if the given parameter value is within the list of possible
   * values.
   * @param value parameter value to check
   * @return the value if it is valid
   */
  private StringParameterValue checkValue(final StringParameterValue value)
  {
    for (final Object choice : getChoices())
    {
      if (choice == null)
      {
        if (value.value == null)
        {
          return value;
        }
      }
      else if (choice.toString().equals(value.value))
      {
        return value;
      }
    }
    throw new IllegalArgumentException("Illegal choice: " + value.value);
  }

  /** Parameter descriptor. */
  @Extension
  public static class DescriptorImpl extends ParameterDescriptor
  {
    @Override
    public final String getDisplayName()
    {
      return ResourceBundleHolder.get(ChoiceParameterDefinition.class).format(
          "DisplayName");
    }
  }
}