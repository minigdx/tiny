-- PICO-8 code for a simple Pong game



function _init()
  -- Set up the game
  -- Game constants
PADDLE_SPEED = 3
BALL_SPEED = 2

-- Game variables
paddle1_y = 60
paddle2_y = 60
ball_x = 255 / 2
ball_y = 255 / 2
ball_x_speed = BALL_SPEED
ball_y_speed = BALL_SPEED

score1 = 0
score2 = 0
end

function _update()
  -- Update the game state
  move_paddles()
  move_ball()
  check_collisions()
end

function _draw()
  -- Draw the game
  cls(1)
  draw_paddles()
  draw_ball()

  print("player one "..score1, 2, 2, 8)
  print("player two "..score2, 2, 8, 8)
end

function btn(b)
  return ctrl.down(b)
end

function  mid(a, b, c)
    return min(max(a, b), c)
end

function move_paddles()
  -- Move the paddles based on player input
  if (btn(0)) then paddle1_y = paddle1_y - PADDLE_SPEED end
  if (btn(1)) then paddle1_y = paddle1_y + PADDLE_SPEED end
  if (btn(2)) then paddle2_y = paddle2_y - PADDLE_SPEED end
  if (btn(3)) then paddle2_y = paddle2_y + PADDLE_SPEED end
  -- Make sure the paddles stay within the screen boundaries
  paddle1_y = mid(0, paddle1_y, 255 - 40)
  paddle2_y = mid(0, paddle2_y, 255 - 40)
end

function move_ball()
  -- Move the ball based on its speed
  ball_x = ball_x + ball_x_speed
  ball_y = ball_y + ball_y_speed
end

function draw_paddles()
  -- Draw the paddles on the screen
  rectf(4, paddle1_y, 8, 40, 7)
  rectf(254 - 4 - 8, paddle2_y, 8, 40, 7)
end

function draw_ball()
  -- Draw the ball on the screen
  circlef(ball_x, ball_y, 8, 7)
end

function check_collisions()
  -- Check for collisions between the ball and the paddles
  if (ball_x < 12 and ball_x > 4 and ball_y > paddle1_y and ball_y < paddle1_y+40) then
    ball_x_speed = BALL_SPEED
  elseif (ball_x > 240 and ball_x < 250 and ball_y > paddle2_y and ball_y < paddle2_y+20) then
    ball_x_speed = -BALL_SPEED
  end

  -- Check for collisions with the screen boundaries
  if (ball_y < 2 or ball_y > 250) then
    ball_y_speed = -ball_y_speed
  end

  -- Check for a point scored
  if (ball_x < 0) then
    -- Player 2 scores a point
    ball_x = 255 / 2
    ball_y = 255 / 2
    score2 = score2 + 10

  elseif (ball_x > 250) then
    -- Player 1 scores a point
    ball_x = 255 / 2
    ball_y = 255 / 2
    score1 = score1 + 1
  end
end
