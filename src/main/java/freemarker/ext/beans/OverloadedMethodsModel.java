/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.ext.beans;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.Collections12;
import freemarker.template.utility.StringUtil;

/**
 * Wraps a set of same-name overloaded methods behind {@link freemarker.template.TemplateMethodModel} interface,
 * like if it was a single method, chooses among them behind the scenes on call-time based on the argument values.
 *  
 * @author Attila Szegedi, szegedia at users dot sourceforge dot net
 */
public class OverloadedMethodsModel
implements
	TemplateMethodModelEx,
	TemplateSequenceModel
{
    private final Object object;
    private final OverloadedMethods overloadedMethods;
    private final BeansWrapper wrapper;
    
    OverloadedMethodsModel(Object object, OverloadedMethods overloadedMethods, BeansWrapper wrapper)
    {
        this.object = object;
        this.overloadedMethods = overloadedMethods;
        this.wrapper = wrapper;
    }

    /**
     * Invokes the method, passing it the arguments from the list. The actual
     * method to call from several overloaded methods will be chosen based
     * on the classes of the arguments.
     * @throws TemplateModelException if the method cannot be chosen
     * unambiguously.
     */
    public Object exec(List arguments)
    throws
        TemplateModelException
    {
        MemberAndArguments maa = overloadedMethods.getMemberAndArguments(arguments, wrapper);
        try {
            return maa.invokeMethod(wrapper, object);
        }
        catch(Exception e)
        {
            while(e instanceof InvocationTargetException)
            {
                Throwable t = ((InvocationTargetException)e).getTargetException();
                if(t instanceof Exception)
                {
                    e = (Exception)t;
                }
                else
                {
                    break;
                }
            }
            if(maa.getCallableMemberDescriptor().isStatic())
            {
                throw new TemplateModelException("Method " + maa.getCallableMemberDescriptor().getDeclaration() + 
                        " threw an exception. See cause exception.", e);
            }
            else
            {
                StringBuffer buf = new StringBuffer();
                Object[] args = maa.getArgs();
                for(int i = 0; i < args.length; ++i)
                {
                    if (i != 0) {
                        buf.append(',');                        
                    }
                    Object arg = args[i];
                    buf.append(arg == null ? "null" : arg.getClass().getName());
                }
                throw new TemplateModelException("Method " + maa.getCallableMemberDescriptor().getDeclaration() + 
                        " threw an exception when invoked on "
                        + object.getClass().getName() + " object "
                        + StringUtil.jQuote(StringUtil.tryToString(object))
                        + " with arguments of types [" + buf + "]. See cause exception.", e);
            }
        }
    }

    public TemplateModel get(int index) throws TemplateModelException
    {
        return (TemplateModel) exec(Collections12.singletonList(
                new SimpleNumber(new Integer(index))));
    }

    public int size() throws TemplateModelException
    {
        throw new TemplateModelException("?size is unsupported for " + getClass().getName());
    }
}
