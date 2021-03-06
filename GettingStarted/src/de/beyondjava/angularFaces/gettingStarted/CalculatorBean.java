package de.beyondjava.angularFaces.gettingStarted;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.context.FacesContext;
import javax.validation.constraints.*;

@ManagedBean
@SessionScoped
public class CalculatorBean implements Serializable {
	private static final long serialVersionUID = 1L;

	@Min(7L)
	@Max(50L)
	@NotNull
	int number1 = 42;

	@Max(100L)
	@Min(10L)
	@NotNull
	int number2 = 65;

	@Max(100L)
	@Min(10L)
	@NotNull
	int number3 = 33;

	int result = 0;

	public String add() {
		result = number1 + number2;
		String msg = "Last calculation on the server side: " + number1 + "+" + number2 + "=" + result;
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg));
		return null;
	}

	/**
	 * @return the number1
	 */
	public int getNumber1() {
		return this.number1;
	}

	/**
	 * @return the number2
	 */
	public int getNumber2() {
		return this.number2;
	}

	public int getNumber3() {
		number3++;
		return number3;
	}

	/**
	 * @return the result
	 */
	public int getResult() {
		return this.result;
	}

	/**
	 * @param number1
	 *            the number1 to set
	 */
	public void setNumber1(int number1) {
		this.number1 = number1;
	}

	/**
	 * @param number2
	 *            the number2 to set
	 */
	public void setNumber2(int number2) {
		this.number2 = number2;
	}

	public void setNumber3(int i) {
		number3 = i;
	}

	/**
	 * @param result
	 *            the result to set
	 */
	public void setResult(int result) {
		this.result = result;
	}
}
