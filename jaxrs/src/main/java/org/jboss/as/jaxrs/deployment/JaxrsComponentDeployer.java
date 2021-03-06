package org.jboss.as.jaxrs.deployment;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.resteasy.util.GetRestful;

/**
 * Integrate's JAX-RS with other component types such as managed beans and EJB's
 *
 * This is not needed if beans.xml is present, as in this case the integration is handed by the more general
 * integration with CDI.
 *
 * @author Stuart Douglas
 */
public class JaxrsComponentDeployer implements DeploymentUnitProcessor {
    private static final Logger log = Logger.getLogger("org.jboss.jaxrs");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
            //this is already handled by the weld integration
            //no need to integrate twice.
            return;
        }
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if(module == null) {
            return;
        }

        final ResteasyDeploymentData resteasy = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);
        if(resteasy == null) {
            return;
        }
        // right now I only support resources
        if (!resteasy.isScanResources()) return;

        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if(moduleDescription == null) {
            return;
        }

        final ClassLoader loader = module.getClassLoader();

        for (final ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            Class<?> componentClass = null;
            try {
                componentClass = loader.loadClass(component.getComponentClassName());
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException(e);
            }
            if (!GetRestful.isRootResource(componentClass)) continue;


            String jndiName = "java:module/" + component.getComponentName();
            log.debug("Found JAX-RS Managed Bean: " + component.getComponentClassName() + " local jndi name: " + jndiName);
            StringBuilder buf = new StringBuilder();
            buf.append(jndiName).append(";").append(component.getComponentClassName()).append(";").append("true");

            resteasy.getScannedJndiComponentResources().add(buf.toString());
            // make sure its removed from list
            resteasy.getScannedResourceClasses().remove(component.getComponentClassName());
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}