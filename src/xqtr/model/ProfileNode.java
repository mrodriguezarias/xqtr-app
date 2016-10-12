package xqtr.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

public class ProfileNode extends ModelNode {

	private LinkedList<ProfileNode> subProfiles = new LinkedList<ProfileNode>();
	private LinkedList<ParameterNode> parameters = new LinkedList<ParameterNode>();
	private ModelNode parent;	//Solo deberia tener Profile o Program
	private Boolean executable = true;

	private void addNewProfile(Element profileNode, HashMap<String, String> variables) {
		this.subProfiles.add(new ProfileNode(this, profileNode, variables));
	}

	private void addNewParameter(Element parameterNode, HashMap<String, String> variables) {
		this.parameters.add(ParameterNode.newParameter(parameterNode, variables));
	}

	protected List<ParameterNode> getParameters() {
		return parameters;
	}

	protected void setUnExecutable(String motivo) {
		if(!executable) {
			executable = false;
			this.logError("Profile " + this.getAttribute("name") + " is not Executable because " + motivo);
			subProfiles.forEach(subProfile -> subProfile.setUnExecutable("parent profile is not Executable"));
		}
	}

	ProfileNode(ModelNode p, Element profileNode, HashMap<String, String> variables){

		HashMap<String, String> declaredVariables;
		parent = p;

		/*Genero el nuevo diccionario de variables para los perfiles*/
		declaredVariables = this.deepCopyVariables(variables);
		declaredVariables.putAll(this.getVariables(profileNode));

		this.initializeAttributes(profileNode, variables);

		this.elementList(profileNode.getElementsByTagName(profileTag)).forEach((subProfileNode) -> {
			this.addNewProfile(subProfileNode, declaredVariables);
		});

		this.elementList(profileNode.getElementsByTagName(parameterTag)).forEach((parameterNode) -> {
			this.addNewParameter(parameterNode, declaredVariables);
		});

		this.checkConsistency();

	}

	protected  List<String> attributesKeys() {

		List<String> attributesKeys = super.attributesKeys();

		attributesKeys.add("args");

		return attributesKeys;
	}

	protected List<String> neccesaryAttributes() {

		List<String> attributesKeys = super.attributesKeys();

		attributesKeys.add("name");

		return attributesKeys;
	}

	protected Boolean isExecutable() {
		return executable;
	}

	protected void checkNeccesaryAttributes() {
		if(!this.neccesaryAttributes().stream().allMatch(attribute -> attributes.containsKey(attribute)))
			this.setUnexecutable("no posee todos los atributos necesarios (" + this.neccesaryAttributes().toString() + ").");
	}

	protected void checkParametersConsistency() {
		if(!parameters.stream().allMatch(parameter -> parameter.isExecutable()))
			this.setUnexecutable("a parameter is not Executable");
	}

	protected void checkConsistency() {
		this.checkNeccesaryAttributes();
		this.checkParametersConsistency();
	}

	private Boolean hasClass(String className) {
		return this.hasAttribute("class") || this.getAttribute("class").contains(className);
	}

	private Boolean isHidden() {
		return this.hasClass("hidden");
	}

	protected List<String> 	getProfilesNames() {

		List<String> profiles = new LinkedList<String>();

		if(!this.isHidden()) profiles.add((this.isExecutable() ? "" : "!") + this.getAttribute("name"));

		subProfiles.forEach(profile -> profiles.addAll(profile.getProfilesNames()));

		return profiles;
	}

	protected ProfileNode getProfile(String profileName) {

		ProfileNode searchedProfile;

		if(this.getAttribute("name") == profileName)
			return this;

		for(ProfileNode prof : subProfiles)
			if((searchedProfile = prof.getProfile(profileName)) != null)
				return searchedProfile;

		return null;
	}

	protected String getCommand() {
		return parent.getCommand() + this.getAttribute("args");
	}
}
