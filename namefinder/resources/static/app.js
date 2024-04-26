function submitForm() {
    fetch(
      "/",
      {method: "POST", body: JSON.stringify({name: nameInput.value, msg: msgInput.value})}
    ).then(response => response.json())
     .then(json => {
      if (json["success"]) msgInput.value = ""
      errorDiv.innerText = json["err"]
    })
  }
 
  function submitMunchkin() {
    console.log("submitMunchkin")
    fetch(
      "/submitMunchkin",
      {method: "POST", body: JSON.stringify({name: nameInput.value, nameRating: nameRatingInput.value})}
    ).then(response => response.json())
     .then(json => {
      if (json["success"]){
       nameInput.value = json["name"]
       NextName.innerHTML = json["nameRender"]
      }
      errorDiv.innerText = json["err"]
    })
  }


  function submitMunchkinLord() {
    console.log("submitMunchkinLord")
    fetch(
      "/submitMunchkinLord",
      {method: "POST", body: JSON.stringify({name: nameInput.value, nameRating: nameRatingInput.value})}
    ).then(response => response.json())
     .then(json => {
      if (json["success"]){
       nameInput.value = json["name"]
       NextName.innerHTML = json["nameRender"]
      }
      errorDiv.innerText = json["err"]
    })
  }
 
/*
  var socket = new WebSocket("ws://" + location.host + "/subscribe");
  socket.onmessage = function (ev) { 
    echo(ev.data)
    //nextName.innerHTML = ev.data 
  }
  */